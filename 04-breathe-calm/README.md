# Breathe — Calm & Sleep (Project 4)

Guided breathing to reduce stress and fall asleep. An expanding/contracting circle
paces your breath through proven patterns (box breathing, 4-7-8, calm, energize).

## Why this instead of a full Yoga/Meditation app
Meditation apps need licensed voice/music tracks — a real cost and a crowded, funded market.
Breathing is **100% generatable**: the animation is math, the sound is synthesized tones
(`ToneGenerator` / simple oscillator), the haptics are built in. Zero asset licensing,
yet it rides the same mental-health demand. Highest margin of the five.

## Core concept
- Pick a pattern → a circle grows on inhale, holds, shrinks on exhale, with a label and timer.
- Optional soft tone + haptic buzz on each phase change; screen stays awake during a session.
- Session lengths (1/3/5/10 min); gentle end chime; streak + minutes-breathed stats.

## Complaint fixes
Breathing/meditation apps get 1★ for *"locked after 3 sessions"* and *"ads right after a calming session ruins it."* We keep core patterns free and **never show an interstitial at session end** — only on the home list, if at all.

## Monetization
- Free: 4 core patterns, all session lengths.
- Premium ($3.99 one-time / $2.99 yr): extra patterns, custom pattern builder, ambient soundscapes, sleep timer, no banner.

## This starter includes
- `BreathingEngine.kt` — patterns + a phase/progress state machine driven by elapsed time.
- `BreatheScreen.kt` — the animated breathing circle bound to the engine.

## Reuses from the flagship
`build.gradle.kts`, `gradle/`, `ui/theme/`, `ads/`, `billing/`, reminder engine (bedtime reminder), DataStore prefs. `applicationId = com.breathe.calm`.
