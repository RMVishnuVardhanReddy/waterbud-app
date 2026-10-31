package com.waterbud.app.backup

import android.content.Context
import android.net.Uri
import com.waterbud.app.data.Cup
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.ThemeMode
import com.waterbud.app.data.UserPrefs
import com.waterbud.app.data.VolumeUnit
import com.waterbud.app.data.WaterLog
import com.waterbud.app.data.WaterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Full backup & restore to a single JSON file the user saves wherever they like —
 * including Google Drive via the system file picker. No backend, no OAuth, no API keys.
 *
 * Android Auto Backup already mirrors app data to the user's Drive automatically;
 * this adds explicit, portable, cross-device backup the user controls.
 */
object BackupManager {

    private const val VERSION = 1

    suspend fun exportTo(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = buildJson(context)
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    suspend fun importFrom(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: return@runCatching false
            applyJson(context, JSONObject(text))
            true
        }.getOrDefault(false)
    }

    private suspend fun buildJson(context: Context): String {
        val repo = WaterRepository(context)
        val prefs = PrefsRepository(context).prefs.first()
        val logs = repo.allLogs()
        val cups = repo.cups().first()

        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("prefs", JSONObject().apply {
            put("goalMl", prefs.goalMl); put("weightKg", prefs.weightKg)
            put("activityLevel", prefs.activityLevel); put("unit", prefs.unit.name)
            put("theme", prefs.theme.name); put("remindersEnabled", prefs.remindersEnabled)
            put("intervalMinutes", prefs.intervalMinutes)
            put("quietStartHour", prefs.quietStartHour); put("quietEndHour", prefs.quietEndHour)
            put("pauseWhenGoalReached", prefs.pauseWhenGoalReached)
            put("silentReminders", prefs.silentReminders)
            put("weekendDifferent", prefs.weekendDifferent)
            put("weekendQuietStartHour", prefs.weekendQuietStartHour)
            put("weekendQuietEndHour", prefs.weekendQuietEndHour)
            put("weatherAdjustEnabled", prefs.weatherAdjustEnabled)
        })

        root.put("cups", JSONArray().apply {
            cups.forEach { c ->
                put(JSONObject().apply {
                    put("label", c.label); put("amountMl", c.amountMl)
                    put("drinkType", c.drinkType); put("sortOrder", c.sortOrder)
                })
            }
        })

        root.put("logs", JSONArray().apply {
            logs.forEach { l ->
                put(JSONObject().apply {
                    put("timestamp", l.timestamp); put("dayKey", l.dayKey)
                    put("amountMl", l.amountMl); put("hydrationMl", l.hydrationMl)
                    put("drinkType", l.drinkType)
                })
            }
        })
        return root.toString()
    }

    private suspend fun applyJson(context: Context, root: JSONObject) {
        val repo = WaterRepository(context)

        val cupsJson = root.optJSONArray("cups") ?: JSONArray()
        val cups = (0 until cupsJson.length()).map { i ->
            val o = cupsJson.getJSONObject(i)
            Cup(
                label = o.optString("label", "Cup"),
                amountMl = o.optInt("amountMl", 250),
                drinkType = o.optString("drinkType", "water"),
                sortOrder = o.optInt("sortOrder", i)
            )
        }

        val logsJson = root.optJSONArray("logs") ?: JSONArray()
        val logs = (0 until logsJson.length()).map { i ->
            val o = logsJson.getJSONObject(i)
            val amount = o.optInt("amountMl", 0)
            WaterLog(
                timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                dayKey = o.optString("dayKey", ""),
                amountMl = amount,
                hydrationMl = o.optInt("hydrationMl", amount),
                drinkType = o.optString("drinkType", "water")
            )
        }.filter { it.dayKey.isNotBlank() }

        repo.restoreData(logs, cups)

        root.optJSONObject("prefs")?.let { p ->
            PrefsRepository(context).importBackup(
                UserPrefs(
                    goalMl = p.optInt("goalMl", 2000),
                    weightKg = p.optInt("weightKg", 70),
                    activityLevel = p.optInt("activityLevel", 1),
                    unit = runCatching { VolumeUnit.valueOf(p.optString("unit", "ML")) }.getOrDefault(VolumeUnit.ML),
                    theme = runCatching { ThemeMode.valueOf(p.optString("theme", "SYSTEM")) }.getOrDefault(ThemeMode.SYSTEM),
                    remindersEnabled = p.optBoolean("remindersEnabled", true),
                    intervalMinutes = p.optInt("intervalMinutes", 90),
                    quietStartHour = p.optInt("quietStartHour", 22),
                    quietEndHour = p.optInt("quietEndHour", 7),
                    pauseWhenGoalReached = p.optBoolean("pauseWhenGoalReached", true),
                    silentReminders = p.optBoolean("silentReminders", false),
                    weekendDifferent = p.optBoolean("weekendDifferent", false),
                    weekendQuietStartHour = p.optInt("weekendQuietStartHour", 23),
                    weekendQuietEndHour = p.optInt("weekendQuietEndHour", 9),
                    weatherAdjustEnabled = p.optBoolean("weatherAdjustEnabled", false)
                )
            )
        }
    }
}
