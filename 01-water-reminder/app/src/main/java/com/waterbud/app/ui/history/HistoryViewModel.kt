package com.waterbud.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterbud.app.data.Achievement
import com.waterbud.app.data.Achievements
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.Streaks
import com.waterbud.app.data.WaterRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class DayBar(val label: String, val totalMl: Int, val goalMl: Int, val metGoal: Boolean) {
    val fraction: Float get() = if (goalMl <= 0) 0f else (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)
}

data class HistoryUiState(
    val days: List<DayBar> = emptyList(),
    val isPremium: Boolean = false,
    val currentStreak: Int = 0,
    val weeklyAverageMl: Int = 0,
    val achievements: List<Achievement> = emptyList()
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WaterRepository(app)
    private val prefs = PrefsRepository(app)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = Dates.dayKeyTicker().flatMapLatest { todayKey ->
        val range = Dates.lastNDays(7)
        val from = Dates.key(range.first())
        val to = Dates.key(range.last())

        combine(
            repo.dailyTotals(from, to),
            repo.allDailyTotals(),
            repo.logCount(),
            repo.totalVolume(),
            prefs.prefs
        ) { weekTotals, allTotals, logCount, volume, p ->
            val byKey = weekTotals.associate { it.dayKey to it.totalHydration }
            val dayLabels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            // Shared "met" rule: today judged by effective goal, past days by base goal.
            val metDays = Streaks.metDayKeys(allTotals, p.goalMl, todayKey, p.effectiveGoalMl)

            val bars = range.map { date ->
                val key = Dates.key(date)
                DayBar(
                    label = dayLabels[date.dayOfWeek.value - 1],
                    totalMl = byKey[key] ?: 0,
                    goalMl = p.goalMl,
                    metGoal = metDays.contains(key)
                )
            }
            // Average over days that actually have intake (not the empty ones).
            val logged = bars.filter { it.totalMl > 0 }
            val avg = if (logged.isEmpty()) 0 else logged.sumOf { it.totalMl } / logged.size

            HistoryUiState(
                days = bars,
                isPremium = p.isPremium,
                currentStreak = Streaks.current(metDays),
                weeklyAverageMl = avg,
                achievements = Achievements.evaluate(
                    allTotals, p.goalMl, logCount, volume, todayKey, p.effectiveGoalMl
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
