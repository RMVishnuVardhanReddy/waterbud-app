# Unkink — Stretch & Desk Mobility (Project 3)

Short guided stretch routines for people who sit all day: neck, back, hips, wrists.
No gym, no equipment, 3–10 minutes. Audience: office workers, remote workers, gamers.

## Core concept
- Pick a routine (e.g. "Desk reset — 5 min", "Lower back — 8 min", "Morning wake-up").
- A **player** runs each stretch with a countdown timer, auto-advances, and shows "next up".
- Voice/beep cues on transitions (TextToSpeech — free, no audio assets to license).
- Progress + streaks; reminders ("time to stretch") reuse the flagship's reminder engine.

## Why it can win (and complaints it fixes)
Stretch apps earn well but 1★ reviews cluster on *"too many ads between every pose"* and *"everything's locked behind a subscription."* We keep **all core routines free**, ads only on the routine-list screen, and never interrupt an active session.

## Asset note (owner's call)
Pose illustrations are the only asset cost. Start with simple line drawings (SVG/vector, cheap) or even emoji + clear text instructions for v1; add nicer art once there's revenue. The **timer engine below is the real product** and is asset-free.

## Monetization
- Free: all basic routines.
- Premium ($4.99 one-time or $2.99/yr): custom routine builder, longer programs, no banner, reminder packs.

## This starter includes
- `RoutineData.kt` — routine/step model + 3 built-in routines.
- `StretchPlayerViewModel.kt` — the countdown + auto-advance + pause/skip engine (StateFlow).

## Reuses from the flagship
`build.gradle.kts`, `gradle/`, `ui/theme/`, `ads/`, `billing/`, `reminder/` (for "time to stretch"), DataStore prefs. `applicationId = com.unkink.stretch`.
