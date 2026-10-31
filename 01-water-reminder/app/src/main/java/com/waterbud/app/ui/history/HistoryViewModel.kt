package com.waterbud.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterbud.app.data.Achievement
import com.waterbud.app.data.Achievements
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.WaterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DayBar(val label: String, val totalMl: Int, val goalMl: Int) {
    val fraction: Float get() = if (goalMl <= 0) 0f else (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)
    val metGoal: Boolean get() = totalMl >= goalMl
}

data class HistoryUiState(
    val days: List<DayBar> = emptyList(),
    val isPremium: Boolean = false,
    val currentStreak: Int = 0,
    val weeklyAverageMl: Int = 0
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WaterRepository(app)
    private val prefs = PrefsRepository(app)

    private val range = Dates.lastNDays(7)
    private val from = Dates.key(range.first())
    private val to = Dates.key(range.last())

    val uiState = combine(repo.dailyTotals(from, to), prefs.prefs) { totals, p ->
        val byKey = totals.associate { it.dayKey to it.totalHydration }
        val dayLabels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val bars = range.map { date ->
            DayBar(
                label = dayLabels[date.dayOfWeek.value - 1],
                totalMl = byKey[Dates.key(date)] ?: 0,
                goalMl = p.goalMl
            )
        }
        val avg = if (bars.isEmpty()) 0 else bars.sumOf { it.totalMl } / bars.size
        HistoryUiState(
            days = bars,
            isPremium = p.isPremium,
            currentStreak = streak(bars),
            weeklyAverageMl = avg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    val achievements = combine(
        repo.allDailyTotals(), repo.logCount(), repo.totalVolume(), prefs.prefs
    ) { daily, count, volume, p ->
        Achievements.evaluate(daily, p.goalMl, count, volume)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Achievement>())

    /** Count consecutive goal-met days ending today. */
    private fun streak(bars: List<DayBar>): Int {
        var s = 0
        for (b in bars.reversed()) { if (b.metGoal) s++ else break }
        return s
    }
}
