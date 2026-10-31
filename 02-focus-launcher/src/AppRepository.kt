package com.focus.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** One launchable app on the device. */
data class AppInfo(val label: String, val packageName: String)

/**
 * Core launcher logic: list every app that has a launcher entry, launch one,
 * and open the system app-info page (used for rename/hide/uninstall).
 */
class AppRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    fun loadApps(hidden: Set<String> = emptySet()): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (android.os.Build.VERSION.SDK_INT >= 33)
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        else
            @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)

        return resolved
            .asSequence()
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != context.packageName }      // don't list ourselves
            .filter { it.packageName !in hidden }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launch(packageName: String) {
        pm.getLaunchIntentForPackage(packageName)?.let { launch ->
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        }
    }

    fun openAppInfo(packageName: String) {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
