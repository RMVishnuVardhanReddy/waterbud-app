# Waterbud — AdMob + Play Store: full publish guide

Follow top to bottom. This is specific to **Waterbud** (`com.waterbud.app`). Menu names in
AdMob/Play Console move occasionally — the flow stays the same.

Time/money: ~2–4 hours of setup; **$25 one-time** for Play. AdMob is free.

---

## PART A — AdMob (ads)

> The app already contains all the ad code (banner, interstitial, rewarded) and a GDPR
> consent gate. You only swap **test IDs → your IDs** and create the consent message.

### A1. Create the AdMob account
1. Go to **https://admob.google.com** → sign in with the Google account you'll use for money.
2. Accept terms, pick country + timezone (⚠️ timezone/currency are permanent).
3. Add payment info later (under **Payments**) — you only get paid after reaching the threshold (~$100).

### A2. Register the app in AdMob
1. AdMob → **Apps → Add app → Android.**
2. "Is it on Google Play?" → **No** (not yet). Name it **Waterbud**.
3. You now get an **App ID** like `ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY` (note the **`~`**). Copy it.

### A3. Create 3 ad units
AdMob → your app → **Ad units → Add ad unit.** Create these three and copy each **unit ID**
(format `ca-app-pub-XXXX/ZZZZ`, note the **`/`**):
| Unit | Format | Used in Waterbud |
|------|--------|------------------|
| `Waterbud Banner` | **Banner** | History & Settings screens |
| `Waterbud Interstitial` | **Interstitial** | 3rd return Home (capped) |
| `Waterbud Rewarded` | **Rewarded** | "Watch ad to unlock themes 7 days" |

### A4. Put your IDs in the code (replace the test IDs)
Open the repo and change these **4 values**:

**1) App ID** — `01-water-reminder/app/src/main/AndroidManifest.xml`
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />   <!-- your App ID (~) -->
```
**2–4) Ad unit IDs** — `01-water-reminder/app/src/main/java/com/waterbud/app/ads/AdManager.kt`
```kotlin
const val BANNER_TEST = "ca-app-pub-XXXX/ZZZZ"         // your Banner unit (/)
const val INTERSTITIAL_TEST = "ca-app-pub-XXXX/ZZZZ"   // your Interstitial unit
const val REWARDED_TEST = "ca-app-pub-XXXX/ZZZZ"       // your Rewarded unit
```
(`BannerAd.kt` reads `AdManager.BANNER_TEST`, so changing `AdManager` is enough.)

> ⚠️ **Ban risk:** never load your REAL ad IDs on your own phone during testing. Keep the test
> IDs while developing; swap to real IDs only for the release you upload. If you must test on your
> device with real IDs, add your device as a **test device** in AdMob first.

### A5. GDPR / consent message (required for EU users)
The app already calls the consent SDK (`ads/ConsentManager.kt`) — you just create the message:
1. AdMob → **Privacy & messaging → GDPR** → **Create message** → select your app → Publish.
2. Do the same for **Other regulations (US states)**.
Without this, EU users see no ads (the app correctly withholds them).

### A6. (Optional but recommended) app-ads.txt
If you have any website/domain, add it to the Play listing and host an `app-ads.txt` that AdMob
gives you (**AdMob → app-ads.txt**). It reduces ad fraud and protects revenue. Skip if no domain.

### A7. Link AdMob ↔ Play (do this AFTER the app is live on Play)
AdMob → your app → **App settings → Link to Google Play.** This unlocks better targeting/revenue.

---

## PART B — Google Play Console (account + app)

### B1. Create the developer account ($25)
1. **https://play.google.com/console** → sign in → **Create account** (Personal or Organization).
2. Pay the **$25** one-time fee.
3. **Identity verification:** Google now requires name/address and sometimes a D-U-N-S number
   (for organizations) and a phone/ID check. Do this early — it can take a day or two.

### B2. Create the app
Play Console → **Create app**:
- App name: **Waterbud**
- Default language, **App**, **Free**
- Accept declarations.

### B3. Create the Premium in-app product (so purchases work)
1. Play Console → your app → **Monetize → Products → In-app products → Create product.**
2. **Product ID must be exactly** `waterbud_premium` (matches `billing/BillingManager.kt`).
3. Set name, description, price → **Activate.**
4. **License testing:** Play Console → **Setup → License testing** → add your Gmail, so you can test
   buying without being charged.
> Note: you usually must upload a build to a track (B6) before products fully work.

---

## PART C — Build the release

### C1. Create your signing key (ONCE — keep it forever)
In **Android Studio**: **Build → Generate Signed Bundle / APK → Android App Bundle → Create new…**
- Choose a keystore path + strong passwords; fill the cert fields.
- ⚠️ **Back up the `.jks` file and passwords in two safe places.** Lose them = you can never update
  the app again.

### C2. Point gradle at the key (so CI/command line can sign) — optional
Create `01-water-reminder/keystore.properties` (already git-ignored) and wire a `signingConfig`
if you want command-line release builds. For the first launch, the Android Studio dialog is enough.

### C3. Build the AAB
Finish the dialog with **build type = release**. Output: an **`.aab`** file
(`app/release/app-release.aab`). That's what Play wants (not an APK).
> Sanity: the CI "release (R8)" job already proves the release build compiles and shrinks cleanly.

---

## PART D — Store listing & policies (Play Console)

Fill every section with a red ✗ until they're green. The big ones:

1. **Store listing:**
   - Short description (≤80 chars) + full description.
   - **App icon** 512×512 PNG.
   - **Feature graphic** 1024×500.
   - **Screenshots:** 2–8 phone shots (take from the emulator/phone). Add a few if you want a Wear listing.
2. **Privacy policy URL (required):** you use ads + notifications + Health Connect + coarse location.
   Host a simple page (free: GitHub Pages, Google Sites, carrd). Must state what you collect and that
   ad partners (Google AdMob) may collect data.
3. **Data safety form:** declare honestly — hydration data stored on device; coarse location used only
   for the weather feature and not shared; AdMob collects device/ads identifiers.
4. **Health apps declaration:** because you integrate **Health Connect**, Play will ask you to declare
   your health-data use and show the privacy policy. Answer that you only **write** hydration.
5. **Content rating** questionnaire → likely "Everyone".
6. **Target audience** (not children), **Ads = Yes**, **News = No**.
7. **Permissions:** be ready to justify `ACCESS_COARSE_LOCATION` (weather) and notifications.

---

## PART E — Test, then publish

1. **Internal testing first:** Play Console → **Testing → Internal testing → Create release** →
   upload the `.aab` → add your email as a tester → **Roll out**.
2. Open the **opt-in link** on your phone, install from Play, and test on the **real device**:
   - app opens (no crash), onboarding, logging, reminders, widget, tile, premium purchase (as license
     tester), themes-via-rewarded-ad, backup/restore, Health Connect toggle.
3. When happy: **Production → Create release** → upload the same `.aab` (bump `versionCode` in
   `app/build.gradle.kts` for each new upload) → fill release notes → **Send for review.**
4. First review typically takes a few days (new accounts can be slower).

---

## PART F — After launch
- Watch **Play Console → Statistics / Ratings / Crashes (Android vitals)** and **AdMob → earnings**.
- Reply to reviews (helps ranking).
- Ship small updates often; each needs a higher `versionCode`.
- Link AdMob ↔ Play (A7) now that it's live.
- Cross-promote your other apps inside Waterbud (free installs) once they exist.

---

## Quick "where is X in the code" map
| Thing | File |
|------|------|
| AdMob App ID | `app/src/main/AndroidManifest.xml` (`APPLICATION_ID` meta-data) |
| Ad unit IDs | `app/src/main/java/com/waterbud/app/ads/AdManager.kt` |
| Consent (UMP) code | `app/src/main/java/com/waterbud/app/ads/ConsentManager.kt` (message made in AdMob) |
| Premium product ID | `app/src/main/java/com/waterbud/app/billing/BillingManager.kt` → `waterbud_premium` |
| App name | `app/src/main/res/values/strings.xml` → `app_name` |
| Package / version | `app/build.gradle.kts` → `applicationId`, `versionCode`, `versionName` |

## Common gotchas
- **"Ad failed to load" with your real IDs on day one** — new AdMob units take a few hours to serve; and they don't serve on an unpublished app until there's traffic. Test with test IDs.
- **Account suspended** — almost always from clicking your own real ads. Don't.
- **Billing shows nothing** — product not **Active**, or you're not a **license tester**, or no build on a track yet.
- **Can't update later** — wrong/lost keystore. Back it up.
