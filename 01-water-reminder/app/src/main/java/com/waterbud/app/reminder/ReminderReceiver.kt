package com.waterbud.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.waterbud.app.MainActivity
import com.waterbud.app.R
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.WaterRepository
import com.waterbud.app.widget.WaterWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderScheduler.ACTION_DRANK -> handleDrank(context)
                    ReminderScheduler.ACTION_SNOOZE -> handleSnooze(context)
                    else -> handleRemind(context)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleSnooze(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
        ReminderScheduler.snooze(context, 30)
    }

    private suspend fun handleDrank(context: Context) {
        WaterRepository(context).addDrink(250, "water")
        WaterWidgetProvider.updateAll(context)
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
        ReminderScheduler.reschedule(context)
    }

    private suspend fun handleRemind(context: Context) {
        val prefs = PrefsRepository(context).prefs.first()
        if (prefs.pauseWhenGoalReached) {
            val total = WaterRepository(context).totalForDay(Dates.todayKey()).first()
            if (total < prefs.effectiveGoalMl) showNotification(context, prefs.overrideDnd, prefs.silentReminders)
        } else {
            showNotification(context, prefs.overrideDnd, prefs.silentReminders)
        }
        ReminderScheduler.scheduleNext(context, prefs)
    }

    private fun showNotification(context: Context, urgent: Boolean, silent: Boolean) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val msg = MESSAGES.random()
        val channel = when {
            urgent -> CHANNEL_ID_URGENT
            silent -> CHANNEL_ID_SILENT
            else -> CHANNEL_ID
        }
        val priority = when {
            urgent -> NotificationCompat.PRIORITY_HIGH
            silent -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val openPi = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val drankPi = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderScheduler.ACTION_DRANK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getBroadcast(
            context, 3,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderScheduler.ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(msg.first)
            .setContentText(msg.second)
            // Default: DEFAULT priority → respects Do Not Disturb, no battery-draining
            // full-screen alert. If the user opted in, we use the urgent channel that
            // can break through DnD (once they grant Notification Policy Access).
            .setPriority(priority)
            .setCategory(if (urgent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(0, context.getString(R.string.action_i_drank), drankPi)
            .addAction(0, "Snooze 30m", snoozePi)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, context.getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = context.getString(R.string.channel_desc) }
            )
            // Urgent channel: higher importance + DnD bypass (effective only once the
            // user grants Notification Policy Access; otherwise it degrades gracefully).
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_URGENT, context.getString(R.string.channel_urgent_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.channel_urgent_desc)
                    if (nm.isNotificationPolicyAccessGranted) setBypassDnd(true)
                }
            )
            // Silent channel: shows in the shade with no sound or vibration.
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID_SILENT, context.getString(R.string.channel_silent_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = context.getString(R.string.channel_silent_desc) }
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "waterbud_reminders"
        const val CHANNEL_ID_URGENT = "waterbud_reminders_urgent"
        const val CHANNEL_ID_SILENT = "waterbud_reminders_silent"
        const val NOTIF_ID = 42

        // Varied, friendly copy so reminders never feel robotic (reviews: "static reminders").
        private val MESSAGES = listOf(
            "Time to hydrate 💧" to "Take a sip — your buddy's got your back.",
            "Water break!" to "A quick glass keeps you sharp and focused.",
            "Stay hydrated 💧" to "Your body called — it wants a little water.",
            "Hey there 👋" to "Small sip now, big difference later.",
            "Hydration check" to "A few gulps and you're back on track."
        )
    }
}
