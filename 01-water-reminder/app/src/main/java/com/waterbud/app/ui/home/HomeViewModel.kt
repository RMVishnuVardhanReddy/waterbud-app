package com.waterbud.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterbud.app.data.Cup
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.WaterLog
import com.waterbud.app.data.WaterRepository
import com.waterbud.app.reminder.ReminderScheduler
import com.waterbud.app.widget.WaterWidgetProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val totalMl: Int = 0,
    val goalMl: Int = 2000,
    val isPremium: Boolean = false,
    val cups: List<Cup> = emptyList(),
    val todayLogs: List<WaterLog> = emptyList(),
    /** Extra ml added to today's goal because it's hot (0 if none / disabled). */
    val weatherBonusMl: Int = 0
) {
    val progress: Float get() = if (goalMl <= 0) 0f else (totalMl.toFloat() / goalMl).coerceIn(0f, 1f)
    val remainingMl: Int get() = (goalMl - totalMl).coerceAtLeast(0)
    val goalReached: Boolean get() = totalMl >= goalMl
    /** Can exceed 100 — over-goal intake is still counted and shown. */
    val percent: Int get() = if (goalMl <= 0) 0 else (totalMl * 100 / goalMl)
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WaterRepository(app)
    private val prefs = PrefsRepository(app)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = Dates.dayKeyTicker().flatMapLatest { today ->
        combine(
            repo.totalForDay(today),
            prefs.prefs,
            repo.cups(),
            repo.logsForDay(today)
        ) { total, p, cups, logs ->
            HomeUiState(total, p.effectiveGoalMl, p.isPremium, cups, logs, p.weatherBonusMl)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun addDrink(amountMl: Int, type: String = "water") = viewModelScope.launch {
        repo.addDrink(amountMl, type)
        // Reschedule so reminders respect the new intake (auto-pause on goal, etc.).
        ReminderScheduler.reschedule(getApplication())
        WaterWidgetProvider.updateAll(getApplication())
    }

    fun undo(log: WaterLog) = viewModelScope.launch {
        repo.undo(log)
        WaterWidgetProvider.updateAll(getApplication())
    }
}
