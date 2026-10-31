# Waterbud — Build & Publish, step by step

A plain-English checklist to take `01-water-reminder/` from code to a live Play Store app,
even if you're new to Android. Do the steps in order. Anything marked ⚠️ is easy to get wrong.

---

## Part 0 — What you need (one-time)
- A computer (Windows/Mac/Linux) with **Android Studio** (latest stable — "Koala" or newer). Free: https://developer.android.com/studio
- A **Google account**.
- **Google Play Console** account — one-time **$25**: https://play.google.com/console (needed only to publish).
- An **AdMob** account (free): https://admob.google.com (needed only if you keep ads).

You do NOT need: a server, a paid API, or a Mac specifically. No coding required for the steps below.

---

## Part 1 — Open and run the app (prove it works)
1. Open **Android Studio** → **Open** → select the folder `01-water-reminder`.
2. Wait for **Gradle sync** to finish (bottom status bar). It downloads everything automatically, including the Gradle wrapper. First time can take several minutes.
   - ⚠️ If it complains it can't find `androidx.health.connect:connect-client:1.1.0-rc03`, click the version in `app/build.gradle.kts` and let Studio suggest the latest, then **Sync** again. (Same idea if the Wear or UMP versions complain.)
3. Create a test device: **Device Manager** → **Add a device** → pick a phone (e.g. Pixel 7, API 34) → Finish.
4. Press the green **▶ Run** button, target `app`. The app installs and launches on the emulator.
5. Walk through it: onboarding goal, tap to add water, open History and Settings. **Ads show Google's test ads — that's expected.**

> You can ship to testers at this point. Everything below is for a public release.

---

## Part 2 — Make the money parts yours (before public release)

### 2a. AdMob (only if keeping ads)
1. In AdMob, **Add app** → Android → create it (or "app not listed yet").
2. Create ad units: one **Banner**, one **Interstitial**, one **Rewarded**. Copy each unit ID.
3. In the code, replace the **test IDs**:
   - `app/src/main/AndroidManifest.xml` → the `com.google.android.gms.ads.APPLICATION_ID` value (your AdMob **app** ID).
   - `app/src/main/java/com/waterbud/app/ads/AdManager.kt` → `BANNER_TEST`, `INTERSTITIAL_TEST`, `REWARDED_TEST`.
   - `app/src/main/java/com/waterbud/app/ads/BannerAd.kt` uses `AdManager.BANNER_TEST`, so changing `AdManager` is enough.
   - ⚠️ **Never run your real ad IDs on your own test device** — Google can ban the account for "invalid traffic." Keep test IDs while testing; swap to real IDs only in the release build.
4. In AdMob → **Privacy & messaging** → create a **GDPR (EEA) consent message** and a **CCPA/US States** message. The app already calls the consent SDK (`ConsentManager`); this is where the message itself is configured.

### 2b. Google Play Billing (Premium unlock)
1. (After your first upload exists in Play Console — see Part 4.) Play Console → your app → **Monetize → Products → In-app products**.
2. Create a product with **Product ID exactly `waterbud_premium`** (this must match `billing/BillingManager.kt`). Set a price. **Activate** it.
3. Add yourself as a **License tester** (Play Console → Setup → License testing) so you can test purchases without being charged.

### 2c. Health Connect & location (already coded — just policy)
- Health Connect sync and the weather-aware goal are optional and opt-in. Nothing to configure in code. You **must** mention them in your privacy policy and Data safety form (Part 4).

---

## Part 3 — App identity (name, icon, package)
- **Package name** (`applicationId = com.waterbud.app` in `app/build.gradle.kts`) is permanent once published. `com.waterbud.app` should be free — Play confirms at upload. If you want a different one, change it here and in the Wear module (`wear/build.gradle.kts`, same value) **before** publishing.
- **Icon:** replace the placeholder. In Studio: right-click `app/res` → **New → Image Asset** → make a launcher icon from your logo.
- **App display name:** `app/src/main/res/values/strings.xml` → `app_name`.

---

## Part 4 — Publish to Google Play

### 4a. Sign the app (create your keystore — keep it safe forever)
1. Studio → **Build → Generate Signed Bundle / APK → Android App Bundle**.
2. **Create new** keystore. Choose a path and strong passwords. ⚠️ **Back up this keystore file and passwords somewhere safe.** If you lose it you can never update the app again.
3. Build type **release**. This produces an **`.aab`** file (Android App Bundle) — that's what Play wants.

### 4b. Create the listing in Play Console
1. Play Console → **Create app** → name, language, "App", free/paid.
2. Fill the required sections (Play guides you):
   - **Store listing:** short + full description, app icon (512×512), feature graphic (1024×500), and **phone screenshots** (take them from the emulator: the `…` controls → Screenshot).
   - **Privacy policy URL** (required — you use ads, notifications, Health Connect, coarse location). A simple hosted page is fine (e.g. a free site or GitHub Pages).
   - **Data safety** form — declare honestly: hydration data stored on device; location used only for weather and not shared; ads SDK data.
   - **Content rating** questionnaire, **Target audience**, **Ads** = Yes.
3. **Release → Testing → Internal testing** → upload your `.aab` → add your email as a tester → roll out. Install via the opt-in link on your phone and test on a **real device**.
4. When happy: **Production → Create release** → upload the `.aab` → submit for review. First review can take a few days.

### 4c. Wear OS app
- The watch app is the `wear` module. Build it as part of the same App Bundle (Play delivers it to watches automatically under the same package). Test it on a **Wear OS emulator** paired with the phone emulator first — the watch↔phone sync needs both installed.

---

## Part 5 — After launch
- Watch **Play Console → Statistics & Ratings** and **AdMob earnings**.
- Reply to reviews; it improves ranking.
- Ship small updates often (bump `versionCode` in `app/build.gradle.kts` each time).
- **Cross-promote** your other apps inside Waterbud once they're live (free installs).

---

## Quick "where do I change X?" map
| Thing | File |
|------|------|
| App name | `app/src/main/res/values/strings.xml` → `app_name` |
| Package / version | `app/build.gradle.kts` (`applicationId`, `versionCode`, `versionName`) |
| Ad IDs | `AndroidManifest.xml` + `ads/AdManager.kt` |
| Premium product ID | `billing/BillingManager.kt` (`waterbud_premium`) |
| Reminder copy | `reminder/ReminderReceiver.kt` (`MESSAGES`) |
| Goal formula | `data/GoalCalculator.kt` |
| Default cups | `data/WaterRepository.kt` (`ensureDefaultCups`) |
| Colors / theme | `ui/theme/Theme.kt` |
