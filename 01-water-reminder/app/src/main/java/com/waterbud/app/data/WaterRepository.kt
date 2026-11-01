package com.waterbud.app.data

import android.content.Context
import com.waterbud.app.health.HealthConnect
import kotlinx.coroutines.flow.first

/**
 * Single source of truth for water data. Combines Room (logs/cups) and
 * computes hydration from the drink coefficient.
 */
class WaterRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = WaterDatabase.get(context).waterDao()

    fun logsForDay(dayKey: String = Dates.todayKey()) = dao.logsForDay(dayKey)
    fun totalForDay(dayKey: String = Dates.todayKey()) = dao.totalForDay(dayKey)
    fun dailyTotals(from: String, to: String) = dao.dailyTotals(from, to)
    fun cups() = dao.cups()

    suspend fun addDrink(amountMl: Int, drinkType: String = "water") {
        val now = System.currentTimeMillis()
        val hydration = Math.round(amountMl * DrinkTypes.coefficient(drinkType)).toInt()
        dao.insertLog(
            WaterLog(
                timestamp = now,
                dayKey = Dates.todayKey(),
                amountMl = amountMl,
                hydrationMl = hydration,
                drinkType = drinkType
            )
        )
        // Mirror the HYDRATION value (not raw volume) to Health Connect so the number
        // matches the app, when the user has enabled sync. Best-effort.
        if (PrefsRepository(appContext).prefs.first().healthSyncEnabled) {
            HealthConnect.writeHydration(appContext, hydration, now)
        }
        // Keep any paired watch in sync (best-effort, no-op if none).
        com.waterbud.app.wear.WearSync.pushToday(appContext)
    }

    suspend fun undo(log: WaterLog) {
        dao.deleteLog(log)
        // Reverse the same side-effects addDrink created, so HC and the watch stay correct.
        if (PrefsRepository(appContext).prefs.first().healthSyncEnabled) {
            HealthConnect.deleteHydrationAt(appContext, log.timestamp)
        }
        com.waterbud.app.wear.WearSync.pushToday(appContext)
    }

    suspend fun allLogs() = dao.allLogs()
    fun allDailyTotals() = dao.allDailyTotals()
    fun logCount() = dao.logCount()
    fun totalVolume() = dao.totalVolume()

    /** Replaces all logs and cups with the given data (used by restore). */
    suspend fun restoreData(logs: List<WaterLog>, cups: List<Cup>) {
        dao.clearAllLogs()
        dao.clearAllCups()
        if (cups.isNotEmpty()) dao.insertCups(cups)
        if (logs.isNotEmpty()) dao.insertLogs(logs)
    }

    suspend fun upsertCup(cup: Cup) = dao.upsertCup(cup)
    suspend fun deleteCup(cup: Cup) = dao.deleteCup(cup)

    /** Seeds default quick-add cups on first launch. */
    suspend fun ensureDefaultCups() {
        if (dao.cupCount() == 0) {
            dao.insertCups(
                listOf(
                    Cup(label = "Glass", amountMl = 250, sortOrder = 0),
                    Cup(label = "Mug", amountMl = 350, sortOrder = 1),
                    Cup(label = "Bottle", amountMl = 500, sortOrder = 2),
                    Cup(label = "Large", amountMl = 750, sortOrder = 3)
                )
            )
        }
    }
}
