#!/usr/bin/env python3
"""
Command-line setup for the Google Analytics 4 property linked to this Firebase project.

  python3 scripts/firebase/ga4.py bootstrap              # one-time: enable APIs, create service account
  python3 scripts/firebase/ga4.py setup [--dry-run]      # key events, custom dimensions/metrics, retention
  python3 scripts/firebase/ga4.py status                 # show what is configured on the property
  python3 scripts/firebase/ga4.py report [--days 28]     # print the standard reports in the terminal
  python3 scripts/firebase/ga4.py restrict-key --sha1 AA:BB:...   # lock the Android API key to this app

Everything is idempotent: existing items are left alone, missing ones are created.
Needs only Python 3 (stdlib) and the gcloud CLI. Definitions live in firebase/ga4_definitions.json.

Auth model: you log in once with `gcloud auth login`. The script uses your login to mint short-lived
tokens for a service account (no key files) that has access to the GA4 property, because Google does
not issue Analytics-scoped tokens to gcloud's own login.
"""
import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DEFINITIONS = os.path.join(ROOT, "firebase", "ga4_definitions.json")
FIREBASERC = os.path.join(ROOT, ".firebaserc")
APP_PACKAGE = "com.rajatnagpure.pcmplayerconverter"
SA_NAME = "ga4-admin"
ADMIN = "https://analyticsadmin.googleapis.com/v1beta"
DATA = "https://analyticsdata.googleapis.com/v1beta"
SCOPE_EDIT = "https://www.googleapis.com/auth/analytics.edit"
SCOPE_READ = "https://www.googleapis.com/auth/analytics.readonly"
REQUIRED_APIS = [
    "analyticsadmin.googleapis.com",
    "analyticsdata.googleapis.com",
    "iamcredentials.googleapis.com",
    "firebase.googleapis.com",
    "apikeys.googleapis.com",
]


class ApiError(Exception):
    def __init__(self, status, body):
        super().__init__("HTTP %s: %s" % (status, body))
        self.status = status
        self.body = body


# ---------------------------------------------------------------- helpers

def die(msg):
    print("\n✗ " + msg, file=sys.stderr)
    sys.exit(1)


def gcloud(*args, capture=True):
    try:
        res = subprocess.run(["gcloud", *args], check=True, text=True,
                             stdout=subprocess.PIPE if capture else None)
    except FileNotFoundError:
        die("gcloud is not installed. Install it with:  brew install --cask google-cloud-sdk")
    except subprocess.CalledProcessError as e:
        die("gcloud %s failed (exit %s)" % (" ".join(args), e.returncode))
    return (res.stdout or "").strip()


def default_project():
    try:
        with open(FIREBASERC) as f:
            return json.load(f)["projects"]["default"]
    except Exception:
        return None


def http(method, url, token, body=None, project=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", "Bearer " + token)
    req.add_header("Content-Type", "application/json")
    if project:
        req.add_header("x-goog-user-project", project)  # bill quota to this project for user creds
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode()
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raise ApiError(e.code, e.read().decode())


def user_token():
    return gcloud("auth", "print-access-token")


def sa_email(project):
    return "%s@%s.iam.gserviceaccount.com" % (SA_NAME, project)


def sa_token(project, scope):
    """Short-lived token for the service account, minted with the user's gcloud login."""
    url = ("https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/%s:generateAccessToken"
           % sa_email(project))
    try:
        res = http("POST", url, user_token(), {"scope": [scope], "lifetime": "3600s"}, project)
    except ApiError as e:
        if e.status in (403, 404):
            die("Could not get a token for %s.\nRun the one-time step first:\n"
                "  python3 scripts/firebase/ga4.py bootstrap\n(%s)" % (sa_email(project), e))
        raise
    return res["accessToken"]


def find_property(project, explicit):
    if explicit:
        return explicit
    url = "https://firebase.googleapis.com/v1beta1/projects/%s/analyticsDetails" % project
    try:
        res = http("GET", url, user_token(), project=project)
    except ApiError as e:
        die("Could not look up the GA4 property for Firebase project '%s'. Pass --property <id> "
            "(GA4 > Admin > Property details > Property ID).\n(%s)" % (project, e))
    prop = res.get("analyticsProperty", {})
    if not prop.get("id"):
        die("Firebase project '%s' has no linked Google Analytics property." % project)
    print("• GA4 property: %s (%s)" % (prop.get("displayName", "?"), prop["id"]))
    return prop["id"].replace("properties/", "")


def ga_call(method, url, token, body=None):
    try:
        return http(method, url, token, body)
    except ApiError as e:
        if e.status == 403:
            die(no_access_help())
        raise


GRANT_STEPS = """  1. Open https://analytics.google.com  →  Admin (gear icon, bottom-left)
  2. In the "Property settings" column: Property → Property access management
  3. Click the blue "+" (top-right) → "Add users"
  4. Email address: {sa}
     Direct roles: Editor     (untick "Notify new users by email")  → Add"""

CURRENT_SA = ["<service account>"]


def no_access_help():
    return ("The service account can't access the GA4 property yet (HTTP 403).\n"
            "Give it access once (about 1 minute):\n" + GRANT_STEPS.format(sa=CURRENT_SA[0]) +
            "\nThen re-run this command.")


def list_all(url, token, key):
    items, page = [], None
    while True:
        res = ga_call("GET", url + ("?pageSize=200" + ("&pageToken=" + page if page else "")), token)
        items += res.get(key, [])
        page = res.get("nextPageToken")
        if not page:
            return items


# ---------------------------------------------------------------- commands

def cmd_bootstrap(args):
    p = args.project
    me = gcloud("config", "get-value", "account")
    if not me:
        die("You are not logged in. Run:  gcloud auth login")
    print("• Logged in as %s, project %s" % (me, p))
    print("• Enabling APIs (takes ~1 min the first time)...")
    gcloud("services", "enable", *REQUIRED_APIS, "--project", p, capture=False)

    existing = gcloud("iam", "service-accounts", "list", "--project", p, "--format=value(email)").split()
    if sa_email(p) not in existing:
        print("• Creating service account %s" % sa_email(p))
        gcloud("iam", "service-accounts", "create", SA_NAME, "--project", p,
               "--display-name=GA4 admin (CLI setup)", capture=False)
    else:
        print("• Service account %s already exists" % sa_email(p))

    print("• Allowing %s to mint tokens for it (no key files are created)" % me)
    gcloud("iam", "service-accounts", "add-iam-policy-binding", sa_email(p), "--project", p,
           "--member=user:" + me, "--role=roles/iam.serviceAccountTokenCreator",
           "--condition=None", "--quiet")
    print("\n✓ Bootstrap done.\n")
    print("LAST MANUAL STEP (Google offers no API for the very first grant):")
    print(GRANT_STEPS.format(sa=sa_email(p)))
    print("\nThen run:  python3 scripts/firebase/ga4.py setup --dry-run   (preview)")
    print("           python3 scripts/firebase/ga4.py setup             (apply)")


def cmd_setup(args):
    defs = json.load(open(DEFINITIONS))
    prop = find_property(args.project, args.property)
    token = sa_token(args.project, SCOPE_EDIT)
    base = "%s/properties/%s" % (ADMIN, prop)
    dry = args.dry_run
    created = skipped = 0

    def create(kind, url, body, label):
        nonlocal created
        created += 1
        if dry:
            print("  + would create %s: %s" % (kind, label))
        else:
            ga_call("POST", url, token, body)
            print("  + created %s: %s" % (kind, label))

    print("\n[1/4] Key events")
    have = {k["eventName"] for k in list_all(base + "/keyEvents", token, "keyEvents")}
    for k in defs["keyEvents"]:
        if k["eventName"] in have:
            skipped += 1
            print("  = exists: %s" % k["eventName"])
        else:
            create("key event", base + "/keyEvents", k, k["eventName"])

    print("\n[2/4] Custom dimensions")
    have = {(d["parameterName"], d["scope"]) for d in list_all(base + "/customDimensions", token, "customDimensions")}
    for d in defs["customDimensions"]:
        if (d["parameterName"], d["scope"]) in have:
            skipped += 1
            print("  = exists: %s (%s)" % (d["parameterName"], d["scope"]))
        else:
            create("dimension", base + "/customDimensions", d, "%s → \"%s\" (%s)" % (d["parameterName"], d["displayName"], d["scope"]))

    print("\n[3/4] Custom metrics")
    have = {m["parameterName"] for m in list_all(base + "/customMetrics", token, "customMetrics")}
    for m in defs["customMetrics"]:
        if m["parameterName"] in have:
            skipped += 1
            print("  = exists: %s" % m["parameterName"])
        else:
            create("metric", base + "/customMetrics", m, "%s → \"%s\" (%s)" % (m["parameterName"], m["displayName"], m["measurementUnit"]))

    print("\n[4/4] Data retention")
    cur = ga_call("GET", base + "/dataRetentionSettings", token)
    want = defs["dataRetention"]
    if cur.get("eventDataRetention") == want:
        print("  = already %s" % want)
    elif dry:
        print("  ~ would change %s → %s" % (cur.get("eventDataRetention"), want))
    else:
        ga_call("PATCH", base + "/dataRetentionSettings?updateMask=eventDataRetention", token,
                {"eventDataRetention": want})
        print("  ~ changed %s → %s" % (cur.get("eventDataRetention"), want))

    verb = "would create" if dry else "created"
    print("\n✓ Done: %d %s, %d already present.%s" % (created, verb, skipped,
          "  (dry run: nothing changed)" if dry else ""))


def cmd_status(args):
    prop = find_property(args.project, args.property)
    token = sa_token(args.project, SCOPE_READ)
    base = "%s/properties/%s" % (ADMIN, prop)
    print("\nKey events:")
    for k in list_all(base + "/keyEvents", token, "keyEvents"):
        print("  - %s (%s)" % (k["eventName"], k.get("countingMethod")))
    print("\nCustom dimensions:")
    for d in list_all(base + "/customDimensions", token, "customDimensions"):
        print("  - %-16s %-6s \"%s\"" % (d["parameterName"], d["scope"], d["displayName"]))
    print("\nCustom metrics:")
    for m in list_all(base + "/customMetrics", token, "customMetrics"):
        print("  - %-16s %-12s \"%s\"" % (m["parameterName"], m.get("measurementUnit"), m["displayName"]))
    r = ga_call("GET", base + "/dataRetentionSettings", token)
    print("\nEvent data retention: %s" % r.get("eventDataRetention"))


def run_report(prop, token, days, dimensions, metrics=("eventCount",), event_filter=None, limit=25):
    body = {
        "dateRanges": [{"startDate": "%ddaysAgo" % days, "endDate": "today"}],
        "dimensions": [{"name": d} for d in dimensions],
        "metrics": [{"name": m} for m in metrics],
        "limit": limit,
        "orderBys": [{"metric": {"metricName": metrics[0]}, "desc": True}],
    }
    if event_filter:
        body["dimensionFilter"] = {"filter": {"fieldName": "eventName",
                                              "inListFilter": {"values": list(event_filter)}}}
    try:
        res = http("POST", "%s/properties/%s:runReport" % (DATA, prop), token, body)
    except ApiError as e:
        if e.status == 400 and "customEvent" in e.body:
            return None  # dimension not registered yet / no data
        if e.status == 403:
            die(no_access_help())
        raise
    return [([v["value"] for v in r.get("dimensionValues", [])], [v["value"] for v in r["metricValues"]])
            for r in res.get("rows", [])]


def print_table(title, headers, rows):
    print("\n■ " + title)
    if rows is None:
        print("  (custom dimension not registered yet — run `setup`, data appears ~24-48h later)")
        return
    if not rows:
        print("  (no data yet)")
        return
    table = [headers] + [(dims + mets)[:len(headers)] for dims, mets in rows]
    widths = [max(len(str(r[i])) for r in table if i < len(r)) for i in range(len(headers))]
    for i, r in enumerate(table):
        print("  " + "  ".join(str(c).ljust(widths[j]) for j, c in enumerate(r)))
        if i == 0:
            print("  " + "  ".join("-" * w for w in widths))


def cmd_report(args):
    prop = find_property(args.project, args.property)
    token = sa_token(args.project, SCOPE_READ)
    d = args.days
    print("Last %d days (GA4 processes data with a delay of a few hours)" % d)

    funnel = ["file_import", "conversion_start", "conversion_complete", "conversion_failed", "conversion_cancelled"]
    rows = run_report(prop, token, d, ["eventName"], ("eventCount", "totalUsers"), funnel)
    print_table("Core funnel", ["event", "events", "users"], rows)
    if rows:
        counts = {r[0][0]: int(r[1][0]) for r in rows}
        starts = counts.get("conversion_start", 0)
        if starts:
            print("  success rate: %.1f%%   failure rate: %.1f%%" % (
                100.0 * counts.get("conversion_complete", 0) / starts,
                100.0 * counts.get("conversion_failed", 0) / starts))

    print_table("Conversions by direction & format", ["direction", "format", "completed"],
                run_report(prop, token, d, ["customEvent:direction", "customEvent:output_format"],
                           event_filter=["conversion_complete"]))
    print_table("Failures by error & stage", ["error_type", "stage", "count"],
                run_report(prop, token, d, ["customEvent:error_type", "customEvent:stage"],
                           event_filter=["conversion_failed"]))
    print_table("Screens", ["screen", "views", "users"],
                run_report(prop, token, d, ["unifiedScreenName"], ("screenPageViews", "totalUsers")))
    print_table("Permission prompts", ["permission", "granted", "count"],
                run_report(prop, token, d, ["customEvent:permission", "customEvent:granted"],
                           event_filter=["permission_result"]))
    print_table("Flood Fill promo", ["event", "count"],
                run_report(prop, token, d, ["eventName"],
                           event_filter=["promo_impression", "promo_click", "promo_dismiss"]))
    print_table("Daily active users", ["date", "active users"],
                sorted(run_report(prop, token, d, ["date"], ("activeUsers",), limit=d + 1) or [],
                       key=lambda r: r[0][0]))


def cmd_restrict_key(args):
    p = args.project
    keys = json.loads(gcloud("services", "api-keys", "list", "--project", p, "--format=json"))
    android = [k for k in keys if "Android key" in k.get("displayName", "")]
    if not android:
        die("No 'Android key (auto created by Firebase)' found in project %s." % p)
    key = android[0]
    key_id = key["name"].split("/")[-1]
    flags = ["--allowed-application=sha1_fingerprint=%s,package_name=%s" % (s.replace(":", "").lower(), APP_PACKAGE)
             for s in args.sha1]
    print("• Restricting \"%s\" (%s) to %s with %d SHA-1(s)" % (key["displayName"], key_id, APP_PACKAGE, len(args.sha1)))
    gcloud("services", "api-keys", "update", key_id, "--project", p, *flags, capture=False)
    print("✓ Done. API restrictions (which Google APIs the key may call) were left unchanged.")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--project", default=default_project(), help="Firebase/GCP project id (default: from .firebaserc)")
    ap.add_argument("--property", help="GA4 property id (default: looked up from the Firebase project)")
    sub = ap.add_subparsers(dest="cmd", required=True)
    sub.add_parser("bootstrap", help="one-time: enable APIs and create the service account")
    s = sub.add_parser("setup", help="create key events, custom dimensions/metrics, set retention")
    s.add_argument("--dry-run", action="store_true", help="only print what would change")
    sub.add_parser("status", help="list current property configuration")
    r = sub.add_parser("report", help="print the standard reports")
    r.add_argument("--days", type=int, default=28)
    k = sub.add_parser("restrict-key", help="restrict the Firebase Android API key to this app")
    k.add_argument("--sha1", action="append", required=True, help="SHA-1 fingerprint; repeat for several")
    args = ap.parse_args()
    if not args.project:
        die("No project id. Pass --project <id> or add .firebaserc")
    CURRENT_SA[0] = sa_email(args.project)
    {"bootstrap": cmd_bootstrap, "setup": cmd_setup, "status": cmd_status,
     "report": cmd_report, "restrict-key": cmd_restrict_key}[args.cmd](args)


if __name__ == "__main__":
    main()
