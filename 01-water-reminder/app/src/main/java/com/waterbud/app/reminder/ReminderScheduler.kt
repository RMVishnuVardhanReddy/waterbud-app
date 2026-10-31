package com.waterbud.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.UserPrefs
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Schedules the NEXT water reminder. We schedule one alarm at a time and
 * re-schedule on each fire — this lets us honor quiet-hours and the
 * interval setting precisely, and avoids battery-draining repeating wakeups.
 */
object ReminderScheduler {

    const val ACTION_REMIND = "com.waterbud.app.REMIND"
    const val ACTION_DRANK = "com.waterbud.app.DRANK"
    const val ACTION_SNOOZE = "com.waterbud.app.SNOOZE"
    private const val REQ_CODE = 1001

    suspend fun reschedule(context: Context) {
        val prefs = PrefsRepository(context).prefs.first()
        if (!prefs.remindersEnabled) {
            cancel(context)
            return
        }
        scheduleNext(context, prefs)
    }

    fun scheduleNext(context: Context, prefs: UserPrefs) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact alarm on purpose: it's battery-friendly, needs no special permission,
        // and avoids the USE_EXACT_ALARM Play policy restriction (reserved for alarm-clock
        // apps). A few minutes' drift is irrelevant for a hydration nudge.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerMillis(prefs), remindIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(remindIntent(context))
    }

    /** Snooze: re-fire the reminder after [minutes] (default 30). */
    fun snooze(context: Context, minutes: Int = 30) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + minutes * 60_000L
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, remindIntent(context))
    }

    private fun remindIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQ_CODE,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    /**
     * Next fire = now + interval, but if that lands inside quiet hours,
     * push it to the quiet-end hour. (Fixes "fires at 3am" complaints.)
     */
    fun nextTriggerMillis(prefs: UserPrefs, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.MINUTE, prefs.intervalMinutes)
        }
        val (quietStart, quietEnd) = quietHoursFor(prefs, cal)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if (isQuiet(hour, quietStart, quietEnd)) {
            // move to quiet-end (today or tomorrow)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            if (hour >= quietStart || quietStart < quietEnd) {
                if (hour >= quietEnd) cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            cal.set(Calendar.HOUR_OF_DAY, quietEnd)
        }
        return cal.timeInMillis
    }

    /** Quiet hours for the candidate day, honoring a separate weekend schedule. */
    private fun quietHoursFor(prefs: UserPrefs, cal: Calendar): Pair<Int, Int> {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val weekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
        return if (prefs.weekendDifferent && weekend)
            prefs.weekendQuietStartHour to prefs.weekendQuietEndHour
        else
            prefs.quietStartHour to prefs.quietEndHour
    }

    /** Handles windows that wrap midnight (e.g. 22 → 7). */
    fun isQuiet(hour: Int, start: Int, end: Int): Boolean =
        if (start <= end) hour in start until end
        else hour >= start || hour < end
}
