# Focus — Minimal Launcher (Project 2)

A distraction-free home screen. Black background, a short text list of only the apps
you choose, swipe for the full drawer, nothing else. Rides the "digital detox" trend.

## Core concept
- **Home** = a tiny list of up to ~6 favorite apps as plain text (no icons, no grid).
- **App drawer** = alphabetical text list of all apps with instant search.
- **Gestures**: swipe up = drawer, swipe down = notifications, double-tap = lock (accessibility, premium).
- **Hide apps**, rename apps, set as default launcher with a clear in-app flow.

## Why it can win (and the complaint it fixes)
Minimalist launchers have loyal users but the top 1★ complaint is *"set it as default and got stuck / couldn't get out."* We fix that with an always-reachable Settings entry and an on-screen "How to change back" helper.

## Monetization
- Free: full launcher.
- Premium ($3.99 one-time): custom fonts, hidden-app PIN, gesture actions, extra clock styles.
- No ads on the home screen ever (it would destroy the minimalist value prop); optional banner only inside Settings.

## This starter includes
- `LauncherManifest.xml` — the HOME-category intent filter that makes it a launcher.
- `AppRepository.kt` — queries installed launchable apps via `PackageManager`.
- `HomeLauncherScreen.kt` — the minimal home + drawer Compose UI with search.

## Reuses from the flagship (`01-water-reminder`)
Copy `build.gradle.kts`, `gradle/`, `ui/theme/`, `ads/`, `billing/`, and the DataStore `PrefsRepository` pattern. Change `applicationId` to `com.focus.launcher`.

## Next steps to ship
1. Build favorites editor + hide/rename UI.
2. Implement swipe gestures (`detectVerticalDragGestures`).
3. Default-launcher prompt via `RoleManager` (API 29+) / intent.
4. Clock + date header, optional weather line.
