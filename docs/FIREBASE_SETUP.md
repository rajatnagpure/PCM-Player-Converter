# Firebase setup (free Spark plan)

The app uses three Firebase products. All of them are free on the **Spark** plan, which needs no billing account.

| Product | What it does | Where it shows up |
|---|---|---|
| **Google Analytics for Firebase** | Records events, funnels, retention and audiences | Firebase console → Analytics, and analytics.google.com |
| **Crashlytics** | Reports crashes, ANRs and non-fatal conversion errors | Firebase console → Crashlytics |
| **Remote Config** | Changes the Flood Fill banner rules without a new release | Firebase console → Remote Config |

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

## 5. Create the Remote Config parameters

Remote Config lets you change the banner rules (on/off, how often it appears) **without releasing a new app version**. The app already has the same defaults built in (`app/src/main/res/xml/remote_config_defaults.xml`), so this step is optional until you want to change something.

The parameters you'll create:

| Parameter name (key) | Data type | Default value | What it controls |
|---|---|---|---|
| `promo_floodfill_enabled` | Boolean | `true` | Set to `false` to turn the banner off for everyone |
| `promo_min_sessions` | Number | `2` | The banner never shows before this app launch, so never on the first launch |
| `promo_cooldown_days` | Number | `3` | Minimum days between launches that show the banner |
| `promo_dismiss_snooze_days` | Number | `14` | Days the banner stays hidden after the user taps ✕ |
| `promo_max_impressions` | Number | `5` | Lifetime number of launches that may show the banner |
| `promo_max_dismissals` | Number | `2` | Hide the banner permanently after this many ✕ taps |

### Option A: in the Firebase console, by hand (about 5 minutes)

1. Open the Firebase console, select **pcm-player-and-converter**, and open **Remote Config** from the left menu. It's under **Run** or **DevOps & Engagement**; you can also use the search box at the top.
2. The first time, click **Create configuration**. Afterwards the button is **Add parameter**.
3. A side panel opens. Fill it in for the **first row** of the table above:
   - **Parameter name (key):** `promo_floodfill_enabled`. Copy it exactly; it's case-sensitive.
   - **Data type:** `Boolean`
   - **Description:** for example "Kill switch for the Flood Fill banner". This is only a note for you.
   - **Default value:** `true`
   - Leave **"Use in-app default"** unticked, and don't add conditions.
4. Click **Save**. The parameter appears in the list, and a yellow bar says you have **unpublished changes**.
5. Click **Add parameter** and repeat steps 3–4 for the other five rows. Use **Data type: Number** for those.
6. When all six are listed, click **Publish changes** at the top, then **Publish** in the dialog. Changes have no effect until you publish.

### Option B: one command with the Firebase CLI

The repo contains the same six parameters in `firebase/remoteconfig.template.json`. `firebase.json` and `.firebaserc` point that file at the project.

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
- **Example: showing it less often.** Raise `promo_cooldown_days` to `7`, then Publish.
- To check what's live, open the **Remote Config** page. It shows each parameter's current value and the version history; click **⋮ → Version history** to roll back.

## 6. Mark key events

Key events (formerly "conversions") are the actions that count as success. Marking them lets GA4 report on them directly and makes them available in funnels.

| Event name | Why it's a key event |
|---|---|
| `conversion_complete` | The app's core value moment |
| `promo_click` | Measures the cross-promotion |
| `recording_saved` | The Generator's value moment |

You can create them **now**, before any data arrives:

1. Open <https://analytics.google.com> and pick the property linked to this Firebase project. The property picker is at the top left.
2. Click **Admin** (⚙ gear, bottom left).
3. In the **Property settings** column, open **Data display → Key events**.
4. Click **New key event**, type `conversion_complete` exactly, and click **Save**.
5. Repeat for `promo_click` and `recording_saved`.

Once events start arriving (up to 24 hours later), you can also do this in the Firebase console: **Analytics → Events**, find the event, and turn on **Mark as key event**.

## 7. Register custom dimensions and metrics

**Why this is needed:** GA4 receives every event parameter, such as `output_format` or `error_type`, but reports and Explorations **can only filter or break down by a parameter after it's registered** as a custom dimension. Data arrives from the moment you register it; anything sent before that can't be reported on, so register these early.

### How to create one

1. Open <https://analytics.google.com>, then **Admin** (⚙) → **Property settings** column → **Data display → Custom definitions**.
2. On the **Custom dimensions** tab, click **Create custom dimension**.
3. Fill in the form with one row from the tables below:
   - **Dimension name:** a readable label, shown in reports. For example `Output format`.
   - **Scope:** `Event` for the first table, `User` for the second.
   - **Description:** optional.
   - **Event parameter** (for Event scope) or **User property** (for User scope): type the **exact parameter name**, for example `output_format`. If the dropdown is empty because no data has arrived yet, just type the name.
4. Click **Save**. Repeat for each row. This takes about 30 seconds per row.

**Event-scoped dimensions.** Set Scope to **Event**. The Spark limit is 50 and these use 18.

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

**User-scoped dimensions.** Set Scope to **User**. The limit is 25.

| Dimension name | User property |
|---|---|
| Theme | `theme` |
| Haptics enabled | `haptics_enabled` |
| Has converted | `has_converted` |

**Custom metrics** are numbers you can sum or average. To create one, go to the same page, open the **Custom metrics** tab, and click **Create custom metric**:

| Metric name | Scope | Event parameter | Unit of measurement |
|---|---|---|---|
| Duration (ms) | Event | `duration_ms` | Milliseconds |
| Duration (s) | Event | `duration_s` | Seconds |
| Impression number | Event | `impression_n` | Standard |

Screen names (`screen_view` → `screen_name`) are built into GA4 as **Page title and screen name**, so they don't need registering.

## 8. Data retention

1. Go to analytics.google.com → **Admin** → **Property settings** → **Data collection and modification → Data retention**.
2. Set **Event data retention** to **14 months**, the maximum on the free tier. The default is 2 months.
3. Click **Save**.

This only limits how far back **Explorations** can go. The standard reports aren't affected.

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

Go to analytics.google.com → **Explore** (left menu) → **Blank**. To get dimensions into an exploration, click **+** next to *Dimensions* or *Metrics* and import the ones you registered in step 7.

- **Core funnel:** choose the **Funnel exploration** template. Add steps `file_import` → `conversion_start` → `conversion_complete`, and set **Breakdown** to *Conversion direction* or *Output format*. This shows where users drop out.
- **Reliability:** in a **Free form** exploration, put **Event name** in rows, filtered to `conversion_start` and `conversion_failed`, with **Event count** as the value. Add **Error type** and **Failure stage** as rows. For stack traces, check Crashlytics → Non-fatals.
- **Feature adoption:** in a **Free form** exploration, use **Page title and screen name** × **Active users**.
- **Recording funnel:** `permission_result` → `recording_start` → `recording_stop` → `recording_saved`.
- **Notification opt-in:** use the event `permission_result`, filter **Permission** to `post_notifications`, and break down by **Permission granted**.
- **Promo CTR:** compare `promo_click` events with `promo_impression` events. Add **Impression number** to see which showing gets clicked.
- **Retention:** the built-in **Reports → Retention**, plus a **Cohort exploration** template.
- **Opened from other apps:** use the event `external_file_open`. It shows how many users arrive from "Share" or "Open with".

Optional: **Admin → Product links → BigQuery links** can export raw events to the free BigQuery sandbox (10 GB of storage and 1 TB of queries per month, no billing). Sandbox tables expire after 60 days.

## 11. Google Play Data safety form

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
| `promo_impression` / `promo_click` / `promo_dismiss` | `promo_id`, `impression_n` | The Flood Fill banner |
