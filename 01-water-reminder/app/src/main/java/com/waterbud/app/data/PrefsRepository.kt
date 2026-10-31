package com.waterbud.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "waterbud_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }
enum class VolumeUnit(val label: String) { ML("ml"), OZ("fl oz") }

/** All user settings in one immutable snapshot. */
data class UserPrefs(
    val onboarded: Boolean = false,
    val goalMl: Int = 2000,
    val weightKg: Int = 70,
    val activityLevel: Int = 1,        // 0 low, 1 normal, 2 high
    val unit: VolumeUnit = VolumeUnit.ML,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val remindersEnabled: Boolean = true,
    val intervalMinutes: Int = 90,
    val quietStartHour: Int = 22,      // 22:00
    val quietEndHour: Int = 7,         // 07:00
    val pauseWhenGoalReached: Boolean = true,
    val isPremium: Boolean = false,
    /** Epoch millis until which rewarded-unlocked themes stay available (0 = none). */
    val rewardThemesUntil: Long = 0L,
    val healthSyncEnabled: Boolean = false,
    /** Opt-in: reminders break through Do Not Disturb (needs policy access). */
    val overrideDnd: Boolean = false,
    val weatherAdjustEnabled: Boolean = false,
    /** Extra ml for hot weather that applies to TODAY only (0 otherwise). */
    val weatherBonusMl: Int = 0,
    /** Reminders post silently (no sound/vibration) when true. */
    val silentReminders: Boolean = false,
    /** Use separate quiet hours on weekends when true. */
    val weekendDifferent: Boolean = false,
    val weekendQuietStartHour: Int = 23,
    val weekendQuietEndHour: Int = 9
) {
    /** Premium forever, or a rewarded ad unlocked themes for today. */
    val themesUnlocked: Boolean get() = isPremium || rewardThemesUntil > System.currentTimeMillis()

    /** The goal actually used today = base goal + today's weather bonus. */
    val effectiveGoalMl: Int get() = goalMl + weatherBonusMl
}

class PrefsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val GOAL = intPreferencesKey("goal_ml")
        val WEIGHT = intPreferencesKey("weight_kg")
        val ACTIVITY = intPreferencesKey("activity")
        val UNIT = stringPreferencesKey("unit")
        val THEME = stringPreferencesKey("theme")
        val REMINDERS = booleanPreferencesKey("reminders")
        val INTERVAL = intPreferencesKey("interval_min")
        val QUIET_START = intPreferencesKey("quiet_start")
        val QUIET_END = intPreferencesKey("quiet_end")
        val PAUSE_ON_GOAL = booleanPreferencesKey("pause_on_goal")
        val PREMIUM = booleanPreferencesKey("premium")
        val REWARD_THEMES_UNTIL = longPreferencesKey("reward_themes_until")
        val HEALTH_SYNC = booleanPreferencesKey("health_sync")
        val OVERRIDE_DND = booleanPreferencesKey("override_dnd")
        val WEATHER_ADJUST = booleanPreferencesKey("weather_adjust")
        val WEATHER_BONUS = intPreferencesKey("weather_bonus")
        val WEATHER_DAY = stringPreferencesKey("weather_day")
        val SILENT = booleanPreferencesKey("silent_reminders")
        val WEEKEND_DIFF = booleanPreferencesKey("weekend_diff")
        val WEEKEND_QUIET_START = intPreferencesKey("weekend_quiet_start")
        val WEEKEND_QUIET_END = intPreferencesKey("weekend_quiet_end")
    }

    val prefs: Flow<UserPrefs> = context.dataStore.data.map { p ->
        UserPrefs(
            onboarded = p[Keys.ONBOARDED] ?: false,
            goalMl = p[Keys.GOAL] ?: 2000,
            weightKg = p[Keys.WEIGHT] ?: 70,
            activityLevel = p[Keys.ACTIVITY] ?: 1,
            unit = runCatching { VolumeUnit.valueOf(p[Keys.UNIT] ?: "ML") }.getOrDefault(VolumeUnit.ML),
            theme = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            remindersEnabled = p[Keys.REMINDERS] ?: true,
            intervalMinutes = p[Keys.INTERVAL] ?: 90,
            quietStartHour = p[Keys.QUIET_START] ?: 22,
            quietEndHour = p[Keys.QUIET_END] ?: 7,
            pauseWhenGoalReached = p[Keys.PAUSE_ON_GOAL] ?: true,
            isPremium = p[Keys.PREMIUM] ?: false,
            rewardThemesUntil = p[Keys.REWARD_THEMES_UNTIL] ?: 0L,
            healthSyncEnabled = p[Keys.HEALTH_SYNC] ?: false,
            overrideDnd = p[Keys.OVERRIDE_DND] ?: false,
            weatherAdjustEnabled = p[Keys.WEATHER_ADJUST] ?: false,
            // Weather bonus only counts if it was stored for today.
            weatherBonusMl = if (p[Keys.WEATHER_DAY] == Dates.todayKey()) (p[Keys.WEATHER_BONUS] ?: 0) else 0,
            silentReminders = p[Keys.SILENT] ?: false,
            weekendDifferent = p[Keys.WEEKEND_DIFF] ?: false,
            weekendQuietStartHour = p[Keys.WEEKEND_QUIET_START] ?: 23,
            weekendQuietEndHour = p[Keys.WEEKEND_QUIET_END] ?: 9
        )
    }

    suspend fun completeOnboarding(goalMl: Int, weightKg: Int, activity: Int) {
        context.dataStore.edit {
            it[Keys.ONBOARDED] = true
            it[Keys.GOAL] = goalMl
            it[Keys.WEIGHT] = weightKg
            it[Keys.ACTIVITY] = activity
        }
    }

    suspend fun setGoal(goalMl: Int) = edit { it[Keys.GOAL] = goalMl }
    suspend fun setUnit(unit: VolumeUnit) = edit { it[Keys.UNIT] = unit.name }
    suspend fun setTheme(theme: ThemeMode) = edit { it[Keys.THEME] = theme.name }
    suspend fun setRemindersEnabled(on: Boolean) = edit { it[Keys.REMINDERS] = on }
    suspend fun setInterval(min: Int) = edit { it[Keys.INTERVAL] = min }
    suspend fun setQuietHours(start: Int, end: Int) =
        edit { it[Keys.QUIET_START] = start; it[Keys.QUIET_END] = end }
    suspend fun setPauseWhenGoalReached(on: Boolean) = edit { it[Keys.PAUSE_ON_GOAL] = on }
    suspend fun setPremium(premium: Boolean) = edit { it[Keys.PREMIUM] = premium }
    suspend fun setRewardThemesUntil(ts: Long) = edit { it[Keys.REWARD_THEMES_UNTIL] = ts }
    suspend fun setHealthSyncEnabled(on: Boolean) = edit { it[Keys.HEALTH_SYNC] = on }
    suspend fun setOverrideDnd(on: Boolean) = edit { it[Keys.OVERRIDE_DND] = on }
    suspend fun setWeatherAdjustEnabled(on: Boolean) = edit { it[Keys.WEATHER_ADJUST] = on }
    suspend fun setWeatherBonus(dayKey: String, ml: Int) =
        edit { it[Keys.WEATHER_DAY] = dayKey; it[Keys.WEATHER_BONUS] = ml }
    suspend fun setSilentReminders(on: Boolean) = edit { it[Keys.SILENT] = on }
    suspend fun setWeekendDifferent(on: Boolean) = edit { it[Keys.WEEKEND_DIFF] = on }
    suspend fun setWeekendQuietHours(start: Int, end: Int) =
        edit { it[Keys.WEEKEND_QUIET_START] = start; it[Keys.WEEKEND_QUIET_END] = end }

    /** Restore settings from a backup. Entitlements (premium, rewards) are NOT restored. */
    suspend fun importBackup(p: UserPrefs) {
        context.dataStore.edit {
            it[Keys.ONBOARDED] = true
            it[Keys.GOAL] = p.goalMl
            it[Keys.WEIGHT] = p.weightKg
            it[Keys.ACTIVITY] = p.activityLevel
            it[Keys.UNIT] = p.unit.name
            it[Keys.THEME] = p.theme.name
            it[Keys.REMINDERS] = p.remindersEnabled
            it[Keys.INTERVAL] = p.intervalMinutes
            it[Keys.QUIET_START] = p.quietStartHour
            it[Keys.QUIET_END] = p.quietEndHour
            it[Keys.PAUSE_ON_GOAL] = p.pauseWhenGoalReached
            it[Keys.SILENT] = p.silentReminders
            it[Keys.WEEKEND_DIFF] = p.weekendDifferent
            it[Keys.WEEKEND_QUIET_START] = p.weekendQuietStartHour
            it[Keys.WEEKEND_QUIET_END] = p.weekendQuietEndHour
            it[Keys.WEATHER_ADJUST] = p.weatherAdjustEnabled
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
