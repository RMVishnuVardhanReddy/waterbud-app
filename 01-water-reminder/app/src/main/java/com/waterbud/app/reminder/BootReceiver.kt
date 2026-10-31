package com.waterbud.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Alarms are cleared on reboot and when the app is updated; re-arm the reminder
 * in both cases so reminders keep working without the user re-opening the app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try { ReminderScheduler.reschedule(context) } finally { pending.finish() }
            }
        }
    }
}
