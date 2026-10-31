# Streaks — Habit Tracker (Project 5)

Build daily habits and keep the streak alive. Dead-simple: a list of habits, one tap to
mark today done, a streak counter, and a GitHub-style completion grid. Evergreen market.

## Core concept
- Add habits (name, emoji, color, how many days/week).
- Each day, tap to mark done; see **current streak** and **best streak**.
- A month grid shows done/missed days at a glance — the thing that keeps people coming back.
- Per-habit reminder time (reuses the flagship reminder engine).

## Why it can win (and complaints it fixes)
Habit apps are huge and simple to build, but 1★ reviews hit *"free version only allows 3 habits"* and *"constant upsell popups."* We allow a **generous free habit count** (e.g. 7) and sell depth (stats, widgets, unlimited) instead of gating the basics.

## Monetization
- Free: up to 7 habits, streaks, grid.
- Premium ($3.99 one-time / $2.99 yr): unlimited habits, home-screen widget, detailed stats, export, themes.
- Banner only on the stats screen.

## This starter includes
- `HabitData.kt` — Room entities (Habit, Completion), DAO, and **streak calculation** (the real logic).
- `HabitsViewModel.kt` — toggle today, compute streaks, expose habit list state.

## Reuses from the flagship
`build.gradle.kts`, `gradle/`, Room setup, `ui/theme/`, `ads/`, `billing/`, reminder engine, DataStore prefs. `applicationId = com.streaks.habit`.
