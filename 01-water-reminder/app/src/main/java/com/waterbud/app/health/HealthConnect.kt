package com.waterbud.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.units.Volume
import java.time.Instant
import java.time.ZoneId

/**
 * Optional Health Connect integration — write hydration to the phone's health hub so
 * it shows up alongside steps/sleep. Users increasingly expect this on Android.
 *
 * Everything is best-effort and guarded: if Health Connect isn't installed, or the
 * permission isn't granted, calls simply no-op instead of crashing.
 */
object HealthConnect {

    val PERMISSIONS = setOf(HealthPermission.getWritePermission(HydrationRecord::class))

    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private fun client(context: Context): HealthConnectClient? =
        if (isAvailable(context)) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null

    /** Contract an Activity launches to request the hydration write permission. */
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasPermission(context: Context): Boolean {
        val c = client(context) ?: return false
        return runCatching {
            c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)
        }.getOrDefault(false)
    }

    /** Writes one hydration record. Silently no-ops without availability/permission. */
    suspend fun writeHydration(context: Context, volumeMl: Int, timeMillis: Long) {
        val c = client(context) ?: return
        if (!hasPermission(context)) return
        runCatching {
            val zone = ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(timeMillis)
            val end = start.plusSeconds(1)
            val offset = zone.rules.getOffset(start)
            c.insertRecords(
                listOf(
                    HydrationRecord(
                        startTime = start,
                        startZoneOffset = offset,
                        endTime = end,
                        endZoneOffset = offset,
                        volume = Volume.milliliters(volumeMl.toDouble())
                    )
                )
            )
        }
    }
}
