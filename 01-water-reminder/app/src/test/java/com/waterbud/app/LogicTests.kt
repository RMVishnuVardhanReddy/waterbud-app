package com.waterbud.app

import com.waterbud.app.data.Achievements
import com.waterbud.app.data.DayTotal
import com.waterbud.app.data.DrinkTypes
import com.waterbud.app.data.GoalCalculator
import com.waterbud.app.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalCalculatorTest {
    @Test fun personalizedGoal_normalActivity() {
        // 70 kg * 35 + 250 (normal) = 2700, rounded to nearest 50.
        assertEquals(2700, GoalCalculator.recommendedMl(70, 1))
    }

    @Test fun personalizedGoal_clampsLow() {
        assertTrue(GoalCalculator.recommendedMl(20, 0) >= 1200)
    }

    @Test fun personalizedGoal_clampsHigh() {
        assertTrue(GoalCalculator.recommendedMl(200, 2) <= 4000)
    }

    @Test fun weatherBonus_tiers() {
        assertEquals(0, GoalCalculator.weatherBonusMl(24.0))
        assertEquals(250, GoalCalculator.weatherBonusMl(25.0))
        assertEquals(500, GoalCalculator.weatherBonusMl(31.0))
        assertEquals(700, GoalCalculator.weatherBonusMl(36.0))
    }
}

class DrinkTypesTest {
    @Test fun coefficients() {
        assertEquals(1.0, DrinkTypes.coefficient("water"), 0.0001)
        assertEquals(0.4, DrinkTypes.coefficient("alcohol"), 0.0001)
        assertEquals(1.0, DrinkTypes.coefficient("unknown-id"), 0.0001) // defaults to water
    }
}

class QuietHoursTest {
    @Test fun wrapsMidnight() {
        // 22:00–07:00 quiet window
        assertTrue(ReminderScheduler.isQuiet(23, 22, 7))
        assertTrue(ReminderScheduler.isQuiet(3, 22, 7))
        assertFalse(ReminderScheduler.isQuiet(7, 22, 7))
        assertFalse(ReminderScheduler.isQuiet(12, 22, 7))
    }

    @Test fun sameDayWindow() {
        assertTrue(ReminderScheduler.isQuiet(13, 9, 17))
        assertFalse(ReminderScheduler.isQuiet(17, 9, 17))
        assertFalse(ReminderScheduler.isQuiet(8, 9, 17))
    }
}

class AchievementsTest {
    @Test fun threeConsecutiveMetDays_earnsStreak3_notStreak7() {
        val goal = 2000
        val daily = listOf(
            DayTotal("2026-01-01", 2100),
            DayTotal("2026-01-02", 2000),
            DayTotal("2026-01-03", 2500)
        )
        val result = Achievements.evaluate(daily, goal, logCount = 6, totalVolumeMl = 6600)
        val streak3 = result.first { it.id == "streak_3" }
        val streak7 = result.first { it.id == "streak_7" }
        assertTrue("3-day streak should be earned", streak3.earned)
        assertFalse("7-day streak should not be earned", streak7.earned)
    }

    @Test fun firstSipEarnedWithAnyLog() {
        val result = Achievements.evaluate(emptyList(), 2000, logCount = 1, totalVolumeMl = 250)
        assertTrue(result.first { it.id == "first_sip" }.earned)
    }

    @Test fun gapBreaksStreak() {
        val goal = 2000
        val daily = listOf(
            DayTotal("2026-01-01", 2100),
            DayTotal("2026-01-03", 2100) // gap on the 2nd
        )
        val result = Achievements.evaluate(daily, goal, logCount = 2, totalVolumeMl = 4200)
        assertFalse(result.first { it.id == "streak_3" }.earned)
    }
}
