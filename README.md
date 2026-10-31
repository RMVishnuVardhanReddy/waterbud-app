# Play Store App Portfolio — 5 revenue apps, one shared codebase

A one-person studio plan: ship **one flagship perfectly**, reuse 80% of its architecture
across four siblings. Full business reasoning in [`_docs/STRATEGY.md`](_docs/STRATEGY.md).

## The 5 projects

| # | App | Folder | Status | Earns via |
|---|-----|--------|--------|-----------|
| 1 | **Waterbud** — Water Reminder | `01-water-reminder/` | ✅ **Complete, ship-ready** | Ads + Premium |
| 2 | **Focus** — Minimal Launcher | `02-focus-launcher/` | 🧩 Starter (launcher core) | Premium |
| 3 | **Unkink** — Stretch & Mobility | `03-stretch-mobility/` | 🧩 Starter (timer engine) | Ads + Premium |
| 4 | **Breathe** — Calm & Sleep | `04-breathe-calm/` | 🧩 Starter (breathing engine) | Ads + Premium |
| 5 | **Streaks** — Habit Tracker | `05-habit-streaks/` | 🧩 Starter (streak engine + Room) | Ads + Premium |

> "Complete" = builds and runs with real features. "Starter" = the unique, hard core is
> written; it reuses the flagship's Gradle/theme/ads/billing/reminder skeleton (each README says exactly what to copy).

## Shared stack (all apps)
Kotlin · Jetpack Compose · Material 3 · MVVM (StateFlow) · Room · DataStore · WorkManager · AdMob · Google Play Billing.

## The design principle behind every app
We mined common 1★ reviews of category leaders and turned each into a design rule:
no ads on the core action, quiet hours for reminders, free basics (sell depth not necessities),
no data loss on update, personalized goals. See the table in `_docs/STRATEGY.md`.

## UI preview
An interactive preview of Waterbud's four screens was published as a Claude artifact
(link is in the chat). `_docs/hydro-ui-preview.html` is the source.

## 📖 Step-by-step build & publish guide
New to Android? Follow **[`_docs/BUILD_AND_PUBLISH.md`](_docs/BUILD_AND_PUBLISH.md)** — it takes you from
installing Android Studio to a live Play Store app, in order, in plain English.

## What only you (the owner) can do
These need your accounts/hardware — I can't do them from here (all detailed in the guide above):
1. **Google Play Console** account ($25 one-time) + **AdMob** account → create app IDs.
2. Build & test on a **real device / Android Studio emulator**.
3. Replace test ad IDs with your own; create the `*_premium` billing products.
4. Host a **privacy policy** and fill the Data safety form.
5. Generate the **signed release `.aab`** and upload.

## Suggested order
Ship **Waterbud** first (it's ready). While it gathers reviews, finish Focus/Streaks (simplest
of the four), then Stretch/Breathe. Cross-promote your own apps inside each (free installs).
