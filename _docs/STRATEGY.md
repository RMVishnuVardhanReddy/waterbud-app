# Play Store Portfolio — Strategy & Research

_Owner's brief. Read this first. Last updated: 2026-10-31._

This document is the business case behind the code. It answers three questions:
1. Which 5 apps we build and **why** (research-backed).
2. What features each needs — specifically, **is a "solo" water reminder enough?** (Answer: no — details below.)
3. How each one makes money, and the **real user complaints** we design around so we beat the incumbents.

---

## 0. The strategy in one paragraph

We are a one-person studio with no content budget. So we only build apps that (a) need **no licensed video/illustration/audio**, (b) create a **daily habit** (daily-open apps retain and monetize far better than one-shot tools), and (c) have **proven, simple monetization** (remove-ads + a few premium unlocks). We ship **one flagship perfectly**, then reuse 80% of its architecture across 4 siblings. Same codebase skeleton → 5 apps → 5 revenue streams. That reuse is the whole point: the second app costs a fraction of the first.

---

## 1. The 5 apps (ranked by risk/reward for a solo dev)

| # | App | Why it wins | Asset cost | Monetization strength |
|---|-----|-------------|-----------|----------------------|
| 1 | **Hydro — Water Reminder** (flagship) | Universal need, daily habit, trivial assets, proven category | Very low (icons only) | High |
| 2 | **Focus — Minimal Launcher** | Loyal niche, viral "digital detox" trend, high retention | Very low | Medium–High |
| 3 | **Unkink — Stretch & Desk Mobility** | Office/back-pain audience, no gym needed | Low (simple pose drawings) | High |
| 4 | **Breathe — Calm & Sleep** | Mental-health boom, pure audio/animation we generate ourselves | Low (we synthesize tones) | High (subscription-friendly) |
| 5 | **Streaks — Habit Tracker** | Huge evergreen market, simplest possible logic | Very low | Medium–High |

**Deliberately NOT chosen:** full Yoga and full Fitness apps. They require real exercise video or licensed pose libraries — that's an asset/legal cost a solo dev can't absorb, and the category is dominated by well-funded apps. Stretch + Breathe capture the same "wellness" money without the content bill.

---

## 2. Is a "solo" Water Reminder enough? — NO. Here's why, and the fix.

A bare "tap a button, get a notification" water app **will not retain users or earn money**. The Play Store is full of them and they churn in 2 days. To win, the minimum viable product needs these feature tiers:

### Tier A — Must-have for launch (table stakes, or users 1-star you)
- **Personalized daily goal** — calculated from body weight, activity level, and climate (not a flat 2000 ml). This is the #1 differentiator.
- **Smart, non-annoying reminders** — respect sleep hours (quiet time window), spacing that adapts to how much you've already drunk, and a one-tap "snooze/skip" from the notification itself.
- **Custom cup sizes & drink types** — let users define their own glass/bottle sizes and log coffee/tea/juice with hydration coefficients.
- **Today ring + instant logging** — one tap from the home screen or a **home-screen widget** (widget is a retention superweapon).
- **History & stats** — daily/weekly/monthly charts. People stay for the graphs.
- **Streaks** — "You've hit your goal 6 days in a row." Habit psychology = retention = revenue.

### Tier B — Retention boosters (ship within first updates)
- Home-screen + lock-screen **widgets** and a **Wear OS / quick-settings tile**.
- **Reminder schedules** (weekday vs weekend, work hours).
- **Themes** (light/dark/AMOLED + accent colors) — cheap to build, users love them, great premium hook.
- **Weather-aware goal** (drink more on hot days) via a free weather API.
- **Achievements / badges.**

### Tier C — Premium / monetization features
- Remove ads.
- Unlimited custom cups & drink types.
- Advanced stats & data export (CSV).
- All themes / AMOLED pure-black.
- Cloud backup & sync.

**Conclusion:** The flagship we're building (`01-water-reminder`) includes **all of Tier A** and the architecture hooks for B and C. That is the real "enough."

---

## 3. The real competitor complaints we design around

Mined from public Play Store 1–3★ reviews of popular hydration/habit/launcher apps. Each is a design rule in our apps.

| Common 1★ complaint | Our design rule (the fix) |
|---|---|
| "**Too many ads**, pops up every time I log a glass" | Never interstitial on the core action. Banners only on secondary screens; interstitial at most once per session and never mid-task. Remove-ads is cheap. |
| "Notifications **fire at 3am** / don't respect sleep" | Hard quiet-hours window, on by default (22:00–07:00). |
| "**Paywall everywhere**, can't even use basic logging" | All Tier-A features are 100% free forever. We only charge for convenience/cosmetics. |
| "**Battery drain**" | Use `WorkManager`/`AlarmManager` correctly; no foreground service, no polling. |
| "Lost all my data after update / **no backup**" | Room DB + export + (premium) cloud backup. Never wipe on migration. |
| "Reminders **keep nagging after I hit my goal**" | Auto-pause reminders once daily goal reached (setting). |
| "Goal is a **fixed number**, not for me" | Weight/activity-based goal calculator in onboarding. |
| "**Ugly / dated UI**" | Material 3, dynamic color, smooth animated progress ring, dark/AMOLED. |
| "**Hard to log quickly**" | One-tap logging from notification, widget, and quick-settings tile. |
| (Launcher) "Can't set as default / **stuck**, no way to exit" | Clear default-launcher flow + always-reachable settings + easy uninstall guidance. |

---

## 4. Monetization model (all 5 apps)

**Hybrid: Ads + Premium.** Industry data is consistent — hybrid beats ads-only or paid-only for utility apps.

- **Free tier:** fully usable. AdMob **banner** on non-core screens (History, Settings). **Interstitial** max once per app-open, only between screens, never on the primary action. **Rewarded** ad to temporarily unlock a premium theme ("watch an ad to try AMOLED for today").
- **Premium (Google Play Billing):**
  - **One-time unlock** (~$4.99) *and* a cheap **yearly subscription** (~$2.99/yr) — offer both; many users prefer a one-time buy.
  - Unlocks: remove ads, all themes, unlimited cups, export, cloud backup.
- **Targets to be realistic:** a well-ranked single utility app can do $50–500/mo. Five of them compounding + cross-promotion between your own apps (free house ads pointing to your other apps) is the actual business. **Cross-promotion between your 5 apps is free user acquisition** — build it in.

### Pre-launch monetization checklist
- [ ] AdMob account + app IDs (use **test IDs** until live — real IDs on invalid traffic = ban).
- [ ] Google Play Console account ($25 one-time).
- [ ] Billing products created in Play Console before first billing test.
- [ ] **Privacy policy** (required — ads + data). Host a simple page.
- [ ] Data safety form filled honestly.
- [ ] Target latest `targetSdk` (Play requirement).

---

## 5. Shared architecture (why the 2nd–5th apps are cheap)

Every app uses the identical stack, copied from the flagship:
- **Kotlin + Jetpack Compose + Material 3**
- **MVVM** (ViewModel + StateFlow)
- **Room** (local data) + **DataStore** (preferences)
- **WorkManager / AlarmManager** (reminders & background)
- **AdMob** + **Google Play Billing** wrappers (shared `AdManager`, `BillingManager`)
- **Single-activity + Compose Navigation**

The flagship's `data/`, `ads/`, `billing/`, `ui/theme/`, and reminder-scheduling code are **drop-in reusable**. Each new app changes mainly the domain logic and screens.

---

## 6. Build order & status

| # | App | This-turn status | Next steps to ship |
|---|-----|------------------|--------------------|
| 1 | Water Reminder | **Complete, ship-ready code** | Add app icon, create AdMob/Billing IDs, test on device, upload |
| 2 | Focus Launcher | Scaffold + core launcher logic | Build app drawer, gestures |
| 3 | Stretch & Mobility | Scaffold + routine/timer engine | Add pose drawings, routines |
| 4 | Breathe & Calm | Scaffold + breathing animation engine | Add sessions, sound synthesis |
| 5 | Habit Streaks | Scaffold + habit/streak data model | Build grid UI, reminders |

See each app's own `README.md` for its specifics.

---

## 7. Research round 2 (2026-10-31) — name check, review cross-check, ad tuning

### 7.1 Name availability — flagship renamed Hydro → **Waterbud**
A Play Store check showed **"Hydro" is taken and crowded**: *Hydro* (com.freshware.hydro) and the popular *Hydro Coach* (100k+ installs, 4.6★), plus several iOS "Hydro" apps. Using it would bury us in search and risk confusion. Candidate checks found **no exact Play Store match for "Waterbud"**, which is brandable ("your water buddy"), so the flagship is now **Waterbud** (`com.waterbud.app`). Final uniqueness is enforced on the **package name** at Play Console upload; also do a quick trademark search.

### 7.2 Review cross-check — what real 1★ reviews say, and our status
Mined from public Play/App Store reviews of leading hydration apps:

| Real complaint (quoted themes) | Our status |
|---|---|
| "Reminders keep coming after I hit my goal" | ✅ Auto-pause when goal reached (default on) |
| "An ad on seemingly every click / can't log without watching an ad" | ✅ No ads on the log action; never on launch |
| "Full-screen alert keeps screen on → drains battery" | ✅ No full-screen intent, no wake lock |
| "Reminder ignores Do Not Disturb / still pings after muting" | ✅ DEFAULT-importance channel respects DnD |
| "Static reminders feel robotic" | ✅ Varied reminder messages |
| "Only logs up to the goal — extra water isn't counted" | ✅ Over-goal counted and shown (e.g. 120%) |
| "Want to type an amount, not just sliders" | ✅ Typed custom-amount dialog |
| "Paywalls on basic features" | ✅ All core features free; sell themes/export/backup |
| "Expected Health Connect / Samsung Health sync" | ✅ Health Connect write sync added |
| "No widget / widget behind paywall" | ✅ Free home-screen widget + Quick Settings tile |

### 7.3 Monetization tuned to "earn without irritating"
AdMob research (format eCPM order: **rewarded > interstitial > native > banner**) + the retention finding that **launch interstitials and forced ads cost day-7 retention** drove this model:
- **Banner** on secondary screens only.
- **Interstitial** only at natural breaks, hard-capped (≤3/day, ≥3 min apart, none in first 90s, none on launch or on logging).
- **Rewarded** opt-in (watch to unlock themes for a day) — best revenue-per-impression, user-chosen.
- **Premium** one-time unlock removes ads entirely.

### 7.4 More feature ideas from reviews (backlog, by priority)
1. ~~**Weekday vs weekend reminder schedules**~~ — ✅ built (separate weekend quiet hours).
2. ~~**Weather-aware goal** (drink more on hot days)~~ — ✅ built (opt-in, Open-Meteo + coarse location).
3. ~~**Wear OS companion**~~ — ✅ built (`wear/` module: progress + quick-add, Data Layer sync).
4. ~~**Achievements/badges**~~ — ✅ built (7 badges from local data, shown on History).
5. ~~**Caffeine/alcohol hydration** nuance~~ — ✅ built (drink coefficients incl. alcohol 0.4).
6. ~~**Backup & restore**~~ — ✅ built (JSON file via system picker → save to Drive; no infra/OAuth). Silent Drive app-folder sync remains an optional OAuth upgrade.
7. ~~**Silent-reminder option**~~ — ✅ built (low-importance channel toggle).

### 7.5 Sources
- https://www.androidauthority.com/best-water-reminder-apps-android-916324/
- https://hydrocoach.com/
- https://justuseapp.com/en/app/1221965482/water-reminder-daily-tracker/reviews
- https://play.google.com/store/apps/details?id=watertracker.waterreminder.watertrackerapp.drinkwater
- https://www.playwire.com/blog/admob-ecpm-benchmarks-what-publishers-should-expect
- https://www.revenuelab.fyi/blog/admob-ecpm-benchmarks-2026
- https://zeely.ai/blog/interstitial-ad-examples/
- https://waterllama.com/ (praised for restraint — our design north star)

---

## 8. Round-3 recheck — edge cases, safety & compliance (2026-10-31)

Triggered by two great product questions ("what if someone *wants* reminders during DnD?" and
"does everyone need the same water intake?"). A full audit found and fixed these:

### Fixed
- **Do Not Disturb is now a user choice.** Default reminders respect DnD; an opt-in "Alert during
  Do Not Disturb" setting uses a high-priority bypass channel (requires Notification Policy Access).
- **Medical safety — suggest, don't prescribe.** Research confirms water needs are individual and
  some conditions require a *lower, doctor-set* limit (kidney disease, dialysis, heart/liver failure),
  while others need more (kidney stones); overhydration/hyponatremia is dangerous. Added a clear
  disclaimer in onboarding + a "Health & safety" card in Settings, and the goal stays fully user-editable.
  The app never claims to give medical advice.
- **Exact alarms removed.** Switched to inexact alarms and dropped `SCHEDULE_EXACT_ALARM` /
  `USE_EXACT_ALARM` — the latter is reserved by Play policy for alarm-clock apps and could cause rejection.
- **Reminders re-arm after app updates**, not just reboots (`MY_PACKAGE_REPLACED`), and on every app launch.
- **Billing bug fixed.** Product id was still `hydro_premium` → now `waterbud_premium`. Billing also now
  **revokes premium** if a purchase is refunded/charged back (reflects real ownership on startup).

### Remaining before launch (owner / next session)
- **GDPR consent (Google UMP)** — ✅ implemented (`ads/ConsentManager.kt`, gated in MainActivity/BannerAd).
  Owner still creates the consent message in AdMob → Privacy & messaging.
- **Medical/health policy** — because we reference health, Play wants a visible privacy policy and honest
  Data safety form; the disclaimer above helps but review Play's Health content policy.
- **Minor known issue:** if the app is left open across midnight, the Home screen keeps the old day until
  reopened (logging still records to the correct day). Low impact; fix by re-keying the day on resume.

### Sources
- https://www.kidney.org/kidney-topics/healthy-hydration-and-your-kidneys
- https://www.medicalnewstoday.com/articles/fluid-restriction-in-heart-failure
- https://www.merckmanuals.com/home/kidney-disorders/water-balance/overhydration
- https://www.cedars-sinai.org/newsroom/real-simple-hydration-is-essential-but-can-you-drink-too-much-water/
- https://www.loyolamedicine.org/newsroom/blog-articles/athletes-drinking-too-much-water-can-be-fatal
- Android docs: exact alarms / `USE_EXACT_ALARM` policy; Notification Policy Access (DnD bypass); Google UMP.
