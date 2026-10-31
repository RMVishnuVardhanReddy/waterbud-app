package com.waterbud.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Weather-aware goal: on hot days we suggest a little extra water.
 *
 * Uses Open-Meteo (free, no API key, commercial use allowed). Everything is
 * best-effort and permission-gated — any failure just leaves today's bonus
 * unchanged, never crashes, and never blocks the UI.
 */
object WeatherManager {

    suspend fun refreshIfEnabled(context: Context) {
        withContext(Dispatchers.IO) {
            runCatching {
                val prefs = PrefsRepository(context)
                val snap = prefs.prefs.first()
                if (!snap.weatherAdjustEnabled) return@runCatching
                if (!hasLocationPermission(context)) return@runCatching
                val loc = lastKnownLocation(context) ?: return@runCatching
                val temp = fetchTemperatureC(loc.first, loc.second) ?: return@runCatching
                prefs.setWeatherBonus(Dates.todayKey(), GoalCalculator.weatherBonusMl(temp))
            }
        }
    }

    private fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Returns (lat, lon) from the best available last-known fix, or null. */
    private fun lastKnownLocation(context: Context): Pair<Double, Double>? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
                LocationManager.GPS_PROVIDER
            )
            providers.asSequence()
                .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull { it.time }
                ?.let { it.latitude to it.longitude }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun fetchTemperatureC(lat: Double, lon: Double): Double? {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon&current=temperature_2m"
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000; readTimeout = 8000; requestMethod = "GET"
        }
        return try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).optJSONObject("current")?.optDouble("temperature_2m")
                ?.takeIf { !it.isNaN() }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}
