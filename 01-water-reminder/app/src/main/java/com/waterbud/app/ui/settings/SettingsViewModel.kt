package com.waterbud.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import com.waterbud.app.backup.BackupManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.waterbud.app.data.Cup
import com.waterbud.app.data.DrinkTypes
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.ThemeMode
import com.waterbud.app.data.UserPrefs
import com.waterbud.app.data.WaterRepository
import com.waterbud.app.data.WeatherManager
import com.waterbud.app.widget.WaterWidgetProvider
import com.waterbud.app.reminder.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = PrefsRepository(app)
    private val repo = WaterRepository(app)

    val uiState = prefs.prefs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserPrefs()
    )

    val cups = repo.cups().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGoal(ml: Int) = viewModelScope.launch { prefs.setGoal(ml) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefs.setTheme(mode) }

    fun setRemindersEnabled(on: Boolean) = viewModelScope.launch {
        prefs.setRemindersEnabled(on); ReminderScheduler.reschedule(getApplication())
    }
    fun setInterval(min: Int) = viewModelScope.launch {
        prefs.setInterval(min); ReminderScheduler.reschedule(getApplication())
    }
    fun setQuietHours(start: Int, end: Int) = viewModelScope.launch {
        prefs.setQuietHours(start, end); ReminderScheduler.reschedule(getApplication())
    }
    fun setPauseWhenGoalReached(on: Boolean) = viewModelScope.launch {
        prefs.setPauseWhenGoalReached(on)
    }

    fun setHealthSyncEnabled(on: Boolean) = viewModelScope.launch { prefs.setHealthSyncEnabled(on) }
    fun setSilentReminders(on: Boolean) = viewModelScope.launch { prefs.setSilentReminders(on) }
    fun setWeekendDifferent(on: Boolean) = viewModelScope.launch {
        prefs.setWeekendDifferent(on); ReminderScheduler.reschedule(getApplication())
    }
    fun setWeekendQuietHours(start: Int, end: Int) = viewModelScope.launch {
        prefs.setWeekendQuietHours(start, end); ReminderScheduler.reschedule(getApplication())
    }
    fun setOverrideDnd(on: Boolean) = viewModelScope.launch { prefs.setOverrideDnd(on) }

    fun setWeatherAdjust(on: Boolean) = viewModelScope.launch {
        prefs.setWeatherAdjustEnabled(on)
        if (on) {
            WeatherManager.refreshIfEnabled(getApplication())
            WaterWidgetProvider.updateAll(getApplication())
        } else {
            prefs.setWeatherBonus(Dates.todayKey(), 0)
            WaterWidgetProvider.updateAll(getApplication())
        }
    }

    // --- Custom cups (free for everyone; "unlimited" is simply not capped) ---
    fun addCup(label: String, amountMl: Int) = viewModelScope.launch {
        val order = (cups.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
        repo.upsertCup(Cup(label = label.ifBlank { "$amountMl ml" }, amountMl = amountMl, sortOrder = order))
    }
    fun deleteCup(cup: Cup) = viewModelScope.launch { repo.deleteCup(cup) }

    fun exportBackup(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        onDone(BackupManager.exportTo(getApplication(), uri))
    }

    fun importBackup(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = BackupManager.importFrom(getApplication(), uri)
        if (ok) {
            ReminderScheduler.reschedule(getApplication())
            WaterWidgetProvider.updateAll(getApplication())
        }
        onDone(ok)
    }

    /** Builds a CSV of all logs and returns a shareable content Uri (premium feature). */
    fun exportCsv(onReady: (Uri?) -> Unit) = viewModelScope.launch {
        val uri = withContext(Dispatchers.IO) {
            runCatching {
                val ctx = getApplication<Application>()
                val logs = repo.allLogs()
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                val sb = StringBuilder("date_time,day,amount_ml,hydration_ml,drink_type\n")
                logs.forEach { l ->
                    val t = Instant.ofEpochMilli(l.timestamp).atZone(ZoneId.systemDefault())
                        .toLocalDateTime().format(fmt)
                    sb.append("$t,${l.dayKey},${l.amountMl},${l.hydrationMl},${DrinkTypes.label(l.drinkType)}\n")
                }
                val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "waterbud_export.csv")
                file.writeText(sb.toString())
                FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            }.getOrNull()
        }
        onReady(uri)
    }
}
