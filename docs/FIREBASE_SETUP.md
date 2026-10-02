# Firebase setup (free Spark plan)

The app uses three Firebase products. All of them are free on the **Spark** plan, which needs no billing account.

| Product | What it does | Where it shows up |
|---|---|---|
| **Google Analytics for Firebase** | Records events, funnels, retention and audiences | Firebase console → Analytics, and analytics.google.com |
| **Crashlytics** | Reports crashes, ANRs and non-fatal conversion errors | Firebase console → Crashlytics |
| **Remote Config** | Changes the Color Shift banner rules without a new release | Firebase console → Remote Config |

The app also builds and runs **without** Firebase. If `app/google-services.json` is missing, the Firebase Gradle plugins are skipped and every analytics call does nothing. The file is listed in `.gitignore` because this repo is public, so each developer keeps their own copy locally.

> **Two Google websites are involved.** The **Firebase console** (console.firebase.google.com) is where you manage the project, Crashlytics and Remote Config. **Google Analytics** (analytics.google.com) is the full GA4 interface, which you need for custom dimensions, key events, data retention and Explorations. Firebase creates the GA4 property for you, so you can reach it from **Firebase console → ⚙ Project settings → Integrations → Google Analytics → Manage**, or from the **"View more in Google Analytics"** links on the Analytics pages.

Progress so far:
- [x] 1. Create the project
- [x] 2. Register the Android app and add `google-services.json`. It's verified: the build picks it up, and on the emulator the first events uploaded successfully.
- [ ] 3. Turn on Crashlytics
- [ ] 4. Restrict the API key
- [ ] 5. Create the Remote Config parameters
- [ ] 6. Mark key events
- [ ] 7. Register custom dimensions and metrics
- [ ] 8. Raise data retention to 14 months
- [ ] 9. Check events in DebugView
- [ ] 10. Build the reports
- [ ] 11. Fill in the Play Data safety form

Every step from 4 to 10 can also be done **from the terminal**. See [Terminal path](#terminal-path-steps-410-without-clicking-around) just below; each section also has an "Option: terminal" block.

---

## Terminal path (steps 4–10 without clicking around)

The terminal path uses these files:

- [`scripts/firebase/ga4.py`](../scripts/firebase/ga4.py): a small Python 3 script (no extra packages) that calls Google's official Analytics Admin API and Data API.
- [`firebase/ga4_definitions.json`](../firebase/ga4_definitions.json): lists every key event, custom dimension, custom metric and the retention setting. The script makes the GA4 property match this file.
- [`firebase/remoteconfig.template.json`](../firebase/remoteconfig.template.json): the Remote Config parameters, deployed with the Firebase CLI.

The script is **safe to re-run**. Anything that already exists is skipped, and `--dry-run` shows what it would change without changing anything.

### 0. One-time preparation (about 10 minutes)

**a) Install the Google Cloud CLI (`gcloud`) and log in.** Your Firebase project is also a Google Cloud project, so `gcloud` can manage it.

```bash
brew install --cask google-cloud-sdk
```

Open a **new** terminal window so `gcloud` is on your PATH, then log in in the browser window that opens. Use the same Google account as the Firebase console:

```bash
gcloud auth login
```

```bash
gcloud config set project pcm-player-and-converter
```

**b) Install the Firebase CLI and log in.** It's already installed on this Mac; check with `firebase --version`.

```bash
npm install -g firebase-tools
```

```bash
firebase login
```

**c) Create the helper service account.**

*Why this is needed:* Google doesn't give the plain `gcloud` login permission to change Google Analytics settings. Instead, the script uses a **service account**, a robot Google account that belongs to your project. Your own login gets permission to borrow its access for one hour at a time, so there are **no password or key files** to store.

Run from the repo root:

```bash
python3 scripts/firebase/ga4.py bootstrap
```

The command:
- enables the needed Google APIs on the project (Analytics Admin, Analytics Data, IAM Credentials, Firebase Management, API Keys)
- creates `ga4-admin@pcm-player-and-converter.iam.gserviceaccount.com`
- lets your account borrow its access

**d) Give the service account access to Google Analytics.** This is the **only click step**. Google has no API for granting the very first access.

1. Open <https://analytics.google.com>. Check the property picker at the top left: it should show the property for this app.
2. Click **Admin** (⚙ gear icon, bottom left).
3. In the **Property settings** column, open **Property → Property access management**.
4. Click the blue **+** button (top right) → **Add users**.
5. For **Email address**, enter `ga4-admin@pcm-player-and-converter.iam.gserviceaccount.com`.
6. Under **Direct roles and data restrictions**, choose **Editor**. Untick **Notify new users by email**, since a robot account can't read email.
7. Click **Add**.

### 1. Run it

```bash
python3 scripts/firebase/ga4.py setup --dry-run
```

```bash
python3 scripts/firebase/ga4.py setup
```

```bash
python3 scripts/firebase/ga4.py status
```

- `setup --dry-run` previews the changes.
- `setup` creates the 3 key events, 21 custom dimensions and 3 custom metrics, and sets retention to 14 months. This covers steps 6, 7 and 8.
- `status` shows what's now configured.

Then deploy the Remote Config parameters (step 5):

```bash
firebase deploy --only remoteconfig
```

Restrict the API key (step 4). Use the SHA-1(s) from `./gradlew signingReport` and from the Play Console:

```bash
python3 scripts/firebase/ga4.py restrict-key --sha1 AA:BB:CC:... --sha1 11:22:33:...
```

Read the reports in your terminal (step 10), once data has arrived:

```bash
python3 scripts/firebase/ga4.py report --days 28
```

**Troubleshooting**

| Message | Fix |
|---|---|
| `gcloud is not installed` | Do step 0a, then open a new terminal window |
| `Could not get a token for ga4-admin@...` | Run `bootstrap` (step 0c) again. Check that `gcloud config get-value account` is the account you used in the Firebase console |
| `HTTP 403 ... can't access the GA4 property` | Step 0d isn't done yet. It can take a few minutes to take effect after you click Add |
| `SERVICE_DISABLED` / `has not been used in project` | Wait 2 minutes after `bootstrap`, which enabled the APIs, then retry |
| `Could not look up the GA4 property` | Pass it explicitly: `python3 scripts/firebase/ga4.py --property 123456789 setup`. The ID is in GA4 → Admin → Property details |

---

## 1. Create the project (done)

1. Go to <https://console.firebase.google.com> and click **Create a project**.
2. Turn **Google Analytics ON**, then pick **Default Account for Firebase**.
3. Stay on the **Spark (no-cost)** plan. Nothing in this app needs Blaze.

## 2. Register the Android app (done)

1. Go to **Project Overview** → **Add app** → Android. Use the package name `com.rajatnagpure.pcmplayerconverter`.
2. Download `google-services.json` and save it as `app/google-services.json`.
3. Optional: add SHA-1 fingerprints. You'll need them for step 4. You can print them with:
   ```bash
   ./gradlew signingReport
   ```
   Add the `debug` SHA-1 from that output. Also add the **App signing key** SHA-1 from Play Console → your app → *Test and release → App integrity → App signing*. To add them in Firebase, go to **⚙ Project settings → General → Your apps → Add fingerprint**.

## 3. Turn on Crashlytics

1. In the Firebase console left menu, open **Crashlytics**. It's under the **Run** or **DevOps & Engagement** group; if you can't find it, use the search box at the top.
2. Click **Enable Crashlytics**.
3. The page now shows *"Add SDK" / "Waiting for data"*. The SDK is already in the app, so skip those steps and **send one report**:
   - **Release build:** install a release build (for example `./gradlew installRelease`, or install from your Play internal test track). Open it, then force-stop it and open it again. Crashlytics uploads reports on the next launch.
   - **Debug build:** install with
     ```bash
     ./gradlew installDebug -PanalyticsInDebug=true
     ```
     Debug builds only send data when built this way.
4. Wait about 5 minutes and refresh. The dashboard switches to *"No crashes"* or shows a trend chart. Conversion errors also appear as **non-fatals**. To see them, click **Filter → Event type → Non-fatals**.

## 4. Restrict the API key (recommended, about 3 minutes)

The `api_key` in `google-services.json` isn't a secret, but locking it to your app stops other people from using it.

1. In the Firebase console go to **⚙ Project settings → General**. Your **Web API key** is listed there.
2. Open <https://console.cloud.google.com/apis/credentials> and make sure the project picker at the top shows **pcm-player-and-converter**.
3. Under **API keys**, click **Android key (auto created by Firebase)**.
4. Under **Application restrictions**, choose **Android apps** → **+ Add**. Enter:
   - Package name: `com.rajatnagpure.pcmplayerconverter`
   - SHA-1 fingerprint: one SHA-1 from step 2.3

   Repeat for each SHA-1.
5. Leave **API restrictions** as Firebase set it, since it already lists the Firebase APIs. Click **Save**.

**Option: terminal.** Use one `--sha1` per fingerprint. Colons are optional.

```bash
python3 scripts/firebase/ga4.py restrict-key --sha1 <debug SHA-1> --sha1 <Play app-signing SHA-1>
```

## 5. Create the Remote Config parameters

Remote Config lets you change the banner rules (on/off, how often it appears) **without releasing a new app version**. The app already has the same defaults built in (`app/src/main/res/xml/remote_config_defaults.xml`), so this step is optional until you want to change something.

The parameters you'll create:

| Parameter name (key) | Data type | Default value | What it controls |
|---|---|---|---|
| `promo_floodfill_enabled` | Boolean | `true` | Set to `false` to turn the banner off for everyone |
| `promo_min_sessions` | Number | `2` | The banner never shows before this app launch, so never on the first launch. Set to `1` to show it from the first launch |
| `promo_dismiss_snooze_days` | Number | `4` | Days until the banner comes back after the user taps ✕ |
| `promo_max_dismissals` | Number | `2` | While this many ✕ taps fall inside the window below, the banner stays hidden |
| `promo_dismiss_window_days` | Number | `14` | The rolling window for `promo_max_dismissals`. The banner returns once the older ✕ tap is more than this many days old |

There's no impression limit, and tapping **Play** never hides the banner. It stays until the user taps ✕ or installs Color Shift.

### Option A: in the Firebase console, by hand (about 5 minutes)

1. Open the Firebase console, select **pcm-player-and-converter**, and open **Remote Config** from the left menu. It's under **Run** or **DevOps & Engagement**; you can also use the search box at the top.
2. The first time, click **Create configuration**. Afterwards the button is **Add parameter**.
3. A side panel opens. Fill it in for the **first row** of the table above:
   - **Parameter name (key):** `promo_floodfill_enabled`. Copy it exactly; it's case-sensitive.
   - **Data type:** `Boolean`
   - **Description:** for example "Kill switch for the Color Shift banner". This is only a note for you.
   - **Default value:** `true`
   - Leave **"Use in-app default"** unticked, and don't add conditions.
4. Click **Save**. The parameter appears in the list, and a yellow bar says you have **unpublished changes**.
5. Click **Add parameter** and repeat steps 3–4 for the other four rows. Use **Data type: Number** for those.
6. When all five are listed, click **Publish changes** at the top, then **Publish** in the dialog. Changes have no effect until you publish.

### Option B: one command with the Firebase CLI

The repo contains the same five parameters in `firebase/remoteconfig.template.json`. `firebase.json` and `.firebaserc` point that file at the project.

```bash
npm install -g firebase-tools
```

```bash
firebase login
```

```bash
firebase deploy --only remoteconfig
```

> `deploy --only remoteconfig` **replaces the whole live template**. If you later add parameters in the console, first pull them into the file with `firebase remoteconfig:get -o firebase/remoteconfig.template.json`.

### How and when changes reach users

- The app fetches new values at most **every 12 hours** (every 1 minute in debug builds). The new values apply on the **next app launch**.
- **Example: turning the banner off.** Set `promo_floodfill_enabled` to `false`, then Publish. It disappears for users within about 12 hours plus one restart.
- **Example: backing off longer after a ✕.** Raise `promo_dismiss_snooze_days` to `7`, then Publish.
- To check what's live, open the **Remote Config** page. It shows each parameter's current value and the version history; click **⋮ → Version history** to roll back.

## 6. Mark key events

### What a key event is, in plain words

Every tap and action in the app is sent to Google Analytics as an **event**. For example, `screen_view` is sent when a screen opens, `file_import` when a file is picked, and `conversion_complete` when a conversion succeeds. GA4 stores all of them, but treats them all as equally important.

A **key event** is an event you tell GA4 means **"the user got real value"**. GA4 used to call these "conversions". Here, "conversion" is about marketing and has nothing to do with audio conversion. Once an event is marked as a key event:

- It gets its own **Key events** column in the standard reports (Reports → Acquisition / Engagement), plus a **key event rate** (the % of users or sessions that reached it).
- The Firebase console's Analytics dashboard shows it in its **Key events** card.
- You can see which traffic sources, countries, devices or app versions lead to it most often.
- If you ever run ads (Google Ads or Firebase campaigns), they can optimize toward it.

Marking an event doesn't change what the app sends, and it doesn't use up any custom dimension. It's only a label on the GA4 side.

### Which events and why

| Event name | Meaning | Why it's a key event |
|---|---|---|
| `conversion_complete` | A PCM or audio file was converted and saved | The main reason the app exists. If this goes down, something is broken |
| `recording_saved` | A recording made in the Generator tab was saved | The Generator's value moment |
| `promo_click` | The user tapped **Play** on the Color Shift banner | Measures whether the cross-promotion works |

**Counting method:** use **Once per event**, which counts every conversion. The other option, *Once per session*, counts at most one per visit and fits sign-ups or purchases better.

**Default value:** leave it empty. Values are for revenue, and this app has none.

### Option: terminal

```bash
python3 scripts/firebase/ga4.py setup
```

Running it once covers steps 6, 7 and 8. The key events come from `keyEvents` in `firebase/ga4_definitions.json`.

### Option: by hand

You can do this **before the app has sent any of these events**. GA4 starts labelling them the moment they arrive.

1. Open <https://analytics.google.com> and select this app's property (top left).
2. Click **Admin** (⚙ bottom left). In the **Property settings** column, open **Data display → Key events**.
3. Click the blue **New key event** button (top right).
4. Type the event name **exactly**: `conversion_complete`. It's lower case with an underscore; any typo creates a key event that never matches.
5. Click **Save**. Leave the default counting method, **Once per event**.
6. Repeat for `recording_saved` and `promo_click`.

Later, once events are arriving, you'll also find the events in the Firebase console under **Analytics → Events**. Each one has a **Mark as key event** switch, and turning it on does the same thing.

### How to check it worked

- **Right away:** the **Admin → Key events** list shows the 3 names. In the terminal, `python3 scripts/firebase/ga4.py status` prints them under *Key events*.
- **Within seconds** (DebugView, step 9): convert a file in a debug build. In DebugView, `conversion_complete` appears with a green **flag icon**, which marks key events.
- **After 24–48 hours:** **Reports → Engagement → Key events** lists them with counts, and the Firebase Analytics dashboard shows them on its **Key events** card.

## 7. Register custom dimensions and metrics

### What they are and why you need them

Each event carries **parameters**: extra details such as `output_format = wav` or `error_type = IOException`. GA4 stores all of them, but reports and Explorations can **only filter or group by a parameter once it's registered** as a **custom dimension** (for text) or a **custom metric** (for numbers you want to sum or average).

**Data before registration can't be reported on.** That's why you should register these early, even before many users have the new version.

- **Event-scoped dimension:** describes one event. Example: which `output_format` *this* conversion used.
- **User-scoped dimension:** describes the user, and comes from a *user property*. Example: their `theme`. You can split any report by it ("do Midnight-theme users convert more?").
- **Custom metric:** a number per event. Example: `duration_ms` lets you chart the average conversion time.

Free-tier limits are 50 event-scoped dimensions, 25 user-scoped dimensions and 50 custom metrics. This app uses 18, 3 and 3.

### Option: terminal

```bash
python3 scripts/firebase/ga4.py setup
```

The definitions live in `customDimensions` and `customMetrics` in `firebase/ga4_definitions.json`. To add a new parameter later, add a row to that file and run `setup` again. Existing ones are skipped.

### Option: by hand

1. Open <https://analytics.google.com> → **Admin** (⚙) → **Property settings** column → **Data display → Custom definitions**.
2. On the **Custom dimensions** tab, click **Create custom dimension**.
3. Fill in the form with one row from the tables below:
   - **Dimension name:** the readable label shown in reports, for example `Output format`. Use letters, numbers, spaces and `_` only, with no brackets.
   - **Scope:** **Event** for the first table, **User** for the second.
   - **Description:** optional.
   - **Event parameter** (Event scope) or **User property** (User scope): type the exact parameter name, for example `output_format`. If the dropdown is empty because no data has arrived yet, just type the name.
4. Click **Save**, and repeat for every row. It takes about 30 seconds per row.

**Event-scoped dimensions** (Scope: **Event**):

| Dimension name | Event parameter | Example values |
|---|---|---|
| Conversion direction | `direction` | `pcm_to_audio`, `audio_to_pcm` |
| Output format | `output_format` | `wav`, `m4a`, `flac` |
| PCM encoding | `encoding` | `bit_8`, `bit_16`, `float_32` |
| Sample rate | `sample_rate` | `44100`, `48000` |
| Channels | `channels` | `1`, `2` |
| Error type | `error_type` | `IOException`, `IllegalStateException` |
| Failure stage | `stage` | `convert`, `copy_out` |
| Import source | `source` | `picker`, `external_intent` |
| Target screen | `target` | `converter`, `generator` |
| File extension | `file_ext` | `pcm`, `wav`, `mp3` |
| File size bucket | `size_bucket` | `<100KB`, `1-10MB` |
| Screen | `screen` | `converter`, `generator` |
| Setting | `setting` | `theme`, `haptics`, `analytics` |
| Setting value | `value` | `true`, `midnight` |
| Drawer item | `item` | `rate`, `share_app` |
| Permission | `permission` | `record_audio`, `post_notifications` |
| Permission granted | `granted` | `true`, `false` |
| Promo ID | `promo_id` | `floodfill` |

**User-scoped dimensions** (Scope: **User**):

| Dimension name | User property |
|---|---|
| Theme | `theme` |
| Haptics enabled | `haptics_enabled` |
| Has converted | `has_converted` |

**Custom metrics.** Use the same page, open the **Custom metrics** tab, and click **Create custom metric**:

| Metric name | Scope | Event parameter | Unit of measurement |
|---|---|---|---|
| Duration ms | Event | `duration_ms` | Milliseconds |
| Duration s | Event | `duration_s` | Seconds |
| Impression number | Event | `impression_n` | Standard |

Screen names (`screen_view` → `screen_name`) are built into GA4 as **Page title and screen name**, so they don't need registering.

### How to check it worked

- Run `python3 scripts/firebase/ga4.py status`, or look at the **Custom definitions** page. All 21 dimensions and 3 metrics should be listed.
- About 24–48 hours after users send data, the new dimensions can be picked in **Explore**, and they appear in `python3 scripts/firebase/ga4.py report`.

## 8. Data retention

By default, GA4 keeps detailed event data for only **2 months**. This affects **Explorations** (funnels, custom tables, `ga4.py report` with dimensions). Standard reports keep their totals regardless. On the free tier, the most you can set is **14 months**, which allows year-over-year comparisons.

**Option: terminal.** `python3 scripts/firebase/ga4.py setup` sets it, using `"dataRetention": "FOURTEEN_MONTHS"` in the definitions file.

**Option: by hand:**
1. Go to analytics.google.com → **Admin** → **Property settings** → **Data collection and modification → Data retention**.
2. Set **Event data retention** to **14 months**.
3. Click **Save**.

The change isn't retroactive: data that has already expired stays deleted.

## 9. Check events live with DebugView

Debug builds normally send nothing, so your testing doesn't pollute real data. To watch events live:

```bash
./gradlew installDebug -PanalyticsInDebug=true
```

```bash
adb shell setprop debug.firebase.analytics.app com.rajatnagpure.pcmplayerconverter
```

1. Open the app and tap around.
2. In the Firebase console, open **Analytics → DebugView**, or in GA4 open **Admin → Data display → DebugView**.
3. Your device appears within about 10 seconds, followed by a live stream of events. Click any event to see its parameters.

When you're done, turn debug mode off:

```bash
adb shell setprop debug.firebase.analytics.app .none.
```

In debug builds every event is also printed to logcat under the tag `Analytics`. Filter Logcat in Android Studio with `tag:Analytics`.

## 10. Reports to build (GA4 → Explore)

**Option: terminal.** This prints the key numbers directly, with no clicking:

```bash
python3 scripts/firebase/ga4.py report --days 28
```

It shows:
- the core funnel with success and failure rates
- conversions by direction and format
- failures by error and stage
- screen views
- permission prompts
- promo impressions and clicks
- daily active users

Sections that use custom dimensions say *"not registered yet"* until step 7 is done and about 24–48 hours of data has arrived.

**Option: by hand (visual):**

Go to analytics.google.com → **Explore** (left menu) → **Blank**. To get dimensions into an exploration, click **+** next to *Dimensions* or *Metrics* and import the ones you registered in step 7.

- **Core funnel:** choose the **Funnel exploration** template. Add steps `file_import` → `conversion_start` → `conversion_complete`, and set **Breakdown** to *Conversion direction* or *Output format*. This shows where users drop out.
- **Reliability:** in a **Free form** exploration, put **Event name** in rows, filtered to `conversion_start` and `conversion_failed`, with **Event count** as the value. Add **Error type** and **Failure stage** as rows. For stack traces, check Crashlytics → Non-fatals.
- **Feature adoption:** in a **Free form** exploration, use **Page title and screen name** × **Active users**.
- **Recording funnel:** `permission_result` → `recording_start` → `recording_stop` → `recording_saved`.
- **Notification opt-in:** use the event `permission_result`, filter **Permission** to `post_notifications`, and break down by **Permission granted**.
- **Promo CTR:** compare `promo_click` events with `promo_impression` events (one impression per app launch that shows the banner). Add **Impression number** to see which showing gets clicked. On `promo_dismiss`, **Setting value** is `snoozed_4d` or `capped_14d`, so you can see how many dismissals hit the 14-day cap.
- **Retention:** the built-in **Reports → Retention**, plus a **Cohort exploration** template.
- **Opened from other apps:** use the event `external_file_open`. It shows how many users arrive from "Share" or "Open with".

Optional: **Admin → Product links → BigQuery links** can export raw events to the free BigQuery sandbox (10 GB of storage and 1 TB of queries per month, no billing). Sandbox tables expire after 60 days.

## 11. Google Play Data safety form

> **Why there's no terminal option:** the Play Developer API can upload Data safety answers, but only as the CSV file that the Play Console form itself exports. Filling in the form once by hand (about 5 minutes) is simpler and less error-prone.


Go to Play Console → your app → **Policy and programs → App content → Data safety → Manage**. Answer:

| Data type | Collected | Shared | Processed ephemerally | Required or optional | Purposes |
|---|---|---|---|---|---|
| App activity → App interactions | Yes | No | No | Optional (users can turn it off in Settings) | Analytics |
| App info & performance → Crash logs, Diagnostics | Yes | No | No | Optional | Analytics, App functionality |
| Device or other IDs | Yes (Firebase installation ID) | No | No | Optional | Analytics |

Other questions:
- **Is all data encrypted in transit?** Yes.
- **Can users request deletion?** Answer according to your privacy policy. Analytics data is deleted automatically when retention expires.
- **Advertising ID:** Play Console → **App content → Advertising ID** → *No*. The app sets `google_analytics_adid_collection_enabled=false`.

Also update your privacy policy to say the app uses Google Analytics for Firebase and Firebase Crashlytics.

---

## Reference: what the app sends

- All event and parameter names are defined in `app/src/main/java/com/rajatnagpure/pcmplayerconverter/analytics/AnalyticsEvents.kt`.
- The app **never** sends file names, paths, URIs or text the user typed. It sends only enums, file extensions, size buckets, durations and exception class names.
- Collection respects the **Settings → Privacy → "Share anonymous usage & crash data"** toggle.

| Event | Parameters | When it fires |
|---|---|---|
| `screen_view` | `screen_name` | Converter, generator, help, player or settings becomes visible |
| `file_import` / `file_import_failed` | `source`, `target`, `file_ext`, `size_bucket` | A file is picked or shared in |
| `external_file_open` | `action`, `mime_group`, `target` | The app is opened via Share or Open with |
| `conversion_start` / `conversion_complete` / `conversion_failed` | `direction`, `output_format`, `sample_rate`, `channels`, `encoding`, `size_bucket`, `duration_ms`, `output_size_bucket`, `error_type`, `stage` | The background conversion runs |
| `conversion_cancelled` | `direction` | The save picker is dismissed |
| `playback_start` / `playback_stop` | `screen`, `sample_rate`, `channels`, `encoding`, `duration_s` | Play Input |
| `recording_start` / `recording_stop` / `recording_failed` / `recording_saved` | `sample_rate`, `channels`, `encoding`, `duration_s`, `error_type`, `size_bucket` | Generator recording |
| `permission_result` | `permission`, `granted` | The microphone or notification prompt is answered |
| `settings_changed` | `setting`, `value` | Theme, haptics or analytics toggle |
| `drawer_action`, `share` | `item` / `method`, `content_type` | A drawer menu item is tapped |
| `promo_impression` / `promo_click` / `promo_dismiss` | `promo_id`, `impression_n`, `value` (dismiss: `snoozed_4d` / `capped_14d`) | The Color Shift banner |
