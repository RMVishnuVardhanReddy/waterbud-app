# Waterbud — Water Reminder & Tracker (Flagship, ship-ready)

A modern, Material 3 hydration app engineered to beat the real complaints people leave
on competitor apps (verified from public Play Store / App Store reviews — see
`../_docs/STRATEGY.md`).

> **Name:** "Waterbud" was chosen after checking the Play Store — the common pick "Hydro"
> is already taken (Hydro, Hydro Coach 100k+). Confirm final availability of the **package
> name** `com.waterbud.app` in Play Console at upload, and do a quick trademark check.

## What's implemented (complete)
- **Personalized goal** from weight + activity (onboarding).
- **Home**: animated progress ring, one-tap quick-add cups, **typed custom amount** with drink types,
  **today's log list with delete** (fix mistakes), and **over-goal is counted & shown** (e.g. "120% · goal reached").
- **Smart reminders** (AlarmManager) with **quiet hours**, **auto-pause when goal reached**, **Snooze 30m** +
  **"I drank"** notification actions, **varied messages**, **silent-reminder option**, **separate weekend hours**, re-armed after reboot.
  No full-screen alerts, no wake locks → **no battery drain**; DEFAULT importance → **respects Do Not Disturb**.
- **History**: 7-day chart, **streaks**, weekly average, **achievements/badges** (Room).
- **Backup & restore**: full data to a JSON file via the system picker — save to **Google Drive** or anywhere (no backend/OAuth). Android Auto Backup also mirrors data to Drive automatically.
- **Wear OS companion** (`wear/` module): watch shows today's progress with +250/+500 quick-add; logs to the phone over the Data Layer.
- **Settings**: goal, reminder interval, quiet hours, pause-on-goal, **manage custom cups (add/delete)**,
  theme (System/Light/Dark/**AMOLED**), **CSV export** (premium), **Health Connect sync** toggle,
  **weather-aware goal** (opt-in: drinks more on hot days via free Open-Meteo + coarse location).
- **Home-screen widget**: live progress + one-tap "+250 ml".
- **Quick Settings tile**: log a glass from the notification shade.
- **Health Connect**: mirrors water into the phone's health hub (best-effort, permission-gated).

## Monetization — "earn without irritating" (research-driven)
Based on AdMob monetization research + review analysis:
- **Banner** — only on History/Settings, never Home.
- **Interstitial** — **never on launch** (the #1 retention killer) and **never on the log action**.
  Shown only at a natural break (returning Home), hard-capped **≤3/day**, ≥3 min apart, not in the first 90s.
  (`ads/AdManager.kt` + `ads/AdFrequency.kt`.)
- **Rewarded** — fully **opt-in**: "watch an ad to unlock all themes for today" (highest eCPM, lowest annoyance).
- **Premium** (Play Billing, one-time): remove ads, all themes, CSV export, cloud backup.

## Tech stack
Kotlin · Jetpack Compose · Material 3 · MVVM (StateFlow) · Room · DataStore · WorkManager/AlarmManager ·
AdMob · Play Billing · Health Connect · Wear OS Data Layer. `minSdk 26` (phone) / `30` (wear), `targetSdk 35`.

> **Note on Google Fit:** Health Connect is Google's replacement for the deprecated Google Fit APIs; syncing to Health Connect is the current path and feeds Fit/Fitbit/etc.

## Build & run
1. Open `01-water-reminder/` in **Android Studio (Koala+)**; let Gradle sync (wrapper jar auto-downloads).
2. Run on an emulator/device (Android 8.0+). Ads use **Google test IDs** — safe as-is.
3. If Gradle can't resolve `androidx.health.connect:connect-client:1.1.0-rc03`, bump it to the latest (one line in `app/build.gradle.kts`).

## Before you publish
- [ ] Replace AdMob **test IDs** (manifest + `AdManager`/`BannerAd`) with your own.
- [x] **GDPR consent (Google UMP)** — implemented (`ads/ConsentManager.kt`, gated in `MainActivity`/`BannerAd`). You still create the consent message in AdMob → Privacy & messaging.
- [ ] Create the `waterbud_premium` in-app product in Play Console (see `billing/BillingManager.kt`).
- [ ] Privacy policy (ads + notifications + Health Connect + coarse location for weather) and Data safety form.
- [ ] Health Connect: Play requires a visible privacy policy and the declared data types; review before release.
- [ ] **Health content:** we show a medical disclaimer and never prescribe; review Play's Health policy.
- [ ] Signed release `.aab`, branded icon, store screenshots.

## Safety & correctness notes (from the audits)
- **Reminders:** inexact alarms (battery-friendly, no exact-alarm permission / Play-policy risk); respect
  Do Not Disturb by default, with an **opt-in** DnD-bypass channel; auto-pause at goal; snooze; re-arm after reboot/update.
- **Not medical advice:** the goal is a suggestion for healthy adults and is fully user-editable; conditions
  like kidney/heart/liver disease may need a lower, doctor-set limit — see the Health & safety card in Settings.
- **Ads & consent:** GDPR/UMP consent runs on launch; ads (banner + preloads) are requested only when consent allows — no ads on launch or on the log action; interstitial capped ≤3/day at natural breaks; rewarded is opt-in.
- **Known minor issue:** app left open across midnight shows the previous day until reopened (logs still record correctly).
