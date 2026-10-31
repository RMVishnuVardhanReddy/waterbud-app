package com.waterbud.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val earned: Boolean,
    /** 0f..1f progress toward earning it. */
    val progress: Float
)

/**
 * Pure, testable achievement logic. Everything is derived from the local log
 * data, so badges are free, offline, and shareable — a cheap retention loop.
 */
object Achievements {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun evaluate(
        dailyTotals: List<DayTotal>,
        goalMl: Int,
        logCount: Int,
        totalVolumeMl: Int
    ): List<Achievement> {
        val metDays = dailyTotals.filter { goalMl > 0 && it.totalHydration >= goalMl }.map { it.dayKey }
        val daysGoalMet = metDays.size
        val bestStreak = longestConsecutive(metDays)

        fun ratio(value: Int, target: Int) = (value.toFloat() / target).coerceIn(0f, 1f)

        return listOf(
            badge("first_sip", "💧", "First sip", "Log your first drink",
                logCount >= 1, ratio(logCount, 1)),
            badge("streak_3", "🔥", "On a roll", "Hit your goal 3 days in a row",
                bestStreak >= 3, ratio(bestStreak, 3)),
            badge("streak_7", "🗓️", "Full week", "Hit your goal 7 days in a row",
                bestStreak >= 7, ratio(bestStreak, 7)),
            badge("streak_30", "👑", "Hydration royalty", "Hit your goal 30 days in a row",
                bestStreak >= 30, ratio(bestStreak, 30)),
            badge("met_10", "🎯", "Goal getter", "Reach your goal on 10 days",
                daysGoalMet >= 10, ratio(daysGoalMet, 10)),
            badge("logs_100", "💯", "Century", "Log 100 drinks",
                logCount >= 100, ratio(logCount, 100)),
            badge("litres_50", "🌊", "Making waves", "Drink 50 litres in total",
                totalVolumeMl >= 50_000, ratio(totalVolumeMl, 50_000))
        )
    }

    private fun badge(
        id: String, emoji: String, title: String, desc: String, earned: Boolean, progress: Float
    ) = Achievement(id, emoji, title, desc, earned, if (earned) 1f else progress)

    /** Longest run of consecutive calendar days among the given day keys. */
    private fun longestConsecutive(dayKeys: List<String>): Int {
        if (dayKeys.isEmpty()) return 0
        val dates = dayKeys.mapNotNull { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }.sorted()
        if (dates.isEmpty()) return 0
        var best = 1
        var run = 1
        for (i in 1 until dates.size) {
            run = if (dates[i - 1].plusDays(1) == dates[i]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}
