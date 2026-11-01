package com.waterbud.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Single source of truth for "which days met the goal" and streak math, shared by
 * the History streak card and the Achievements engine so they can never disagree.
 *
 * Consistency rule: a past day counts as met at the base [goalMl]; TODAY counts at
 * [effectiveGoalToday] (goal + any hot-day bonus), so the chart, streak and Home
 * all judge today the same way.
 */
object Streaks {
    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun metDayKeys(
        totals: List<DayTotal>,
        goalMl: Int,
        todayKey: String,
        effectiveGoalToday: Int
    ): Set<String> {
        if (goalMl <= 0) return emptySet()
        return totals.filter {
            val threshold = if (it.dayKey == todayKey) effectiveGoalToday else goalMl
            it.totalHydration >= threshold
        }.map { it.dayKey }.toSet()
    }

    /** Consecutive met days ending today (or yesterday, so a run survives until the day ends). */
    fun current(metDays: Set<String>, today: LocalDate = LocalDate.now()): Int {
        var day = if (metDays.contains(today.format(fmt))) today else today.minusDays(1)
        var streak = 0
        while (metDays.contains(day.format(fmt))) { streak++; day = day.minusDays(1) }
        return streak
    }

    /** Longest run of consecutive met days ever. */
    fun longest(metDays: Set<String>): Int {
        if (metDays.isEmpty()) return 0
        val dates = metDays.mapNotNull { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }.sorted()
        if (dates.isEmpty()) return 0
        var best = 1; var run = 1
        for (i in 1 until dates.size) {
            run = if (dates[i - 1].plusDays(1) == dates[i]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}
