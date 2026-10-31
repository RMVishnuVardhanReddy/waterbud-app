package com.waterbud.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Personalized daily hydration goal — the #1 feature competitors get wrong.
 * Base ≈ 35 ml per kg of body weight, adjusted for activity level.
 * Result is clamped to a sensible, safe range and rounded to the nearest 50 ml.
 */
object GoalCalculator {
    fun recommendedMl(weightKg: Int, activityLevel: Int): Int {
        val base = weightKg * 35.0
        val activityBonus = when (activityLevel) {
            0 -> 0.0        // low / sedentary
            2 -> 500.0      // high / athlete
            else -> 250.0   // normal
        }
        val raw = base + activityBonus
        val clamped = raw.coerceIn(1200.0, 4000.0)
        return (Math.round(clamped / 50.0) * 50).toInt()
    }

    /**
     * Extra ml to drink on a hot day, by current temperature (°C).
     * Conservative, capped — a nudge, not a medical prescription.
     */
    fun weatherBonusMl(temperatureC: Double): Int = when {
        temperatureC >= 35 -> 700
        temperatureC >= 30 -> 500
        temperatureC >= 25 -> 250
        else -> 0
    }
}

/** Hydration coefficients: not every drink counts as pure water. */
object DrinkTypes {
    data class Drink(val id: String, val label: String, val coefficient: Double)

    val all = listOf(
        Drink("water", "Water", 1.0),
        Drink("sparkling", "Sparkling", 1.0),
        Drink("tea", "Tea", 0.9),
        Drink("milk", "Milk", 0.9),
        Drink("juice", "Juice", 0.85),
        Drink("coffee", "Coffee", 0.8),
        Drink("soda", "Soda", 0.7),
        // Alcohol is net dehydrating — it counts for much less, nudging honesty.
        Drink("alcohol", "Alcohol", 0.4)
    )

    fun coefficient(id: String): Double = all.firstOrNull { it.id == id }?.coefficient ?: 1.0
    fun label(id: String): String = all.firstOrNull { it.id == id }?.label ?: "Water"
}

/** Local-date helpers so day grouping is consistent everywhere. */
object Dates {
    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    fun todayKey(): String = LocalDate.now().format(fmt)
    fun key(date: LocalDate): String = date.format(fmt)
    fun lastNDays(n: Int): List<LocalDate> {
        val today = LocalDate.now()
        return (n - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    /** Epoch millis at the end of today (local), used for "unlocked for today" perks. */
    fun endOfTodayMillis(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
}
