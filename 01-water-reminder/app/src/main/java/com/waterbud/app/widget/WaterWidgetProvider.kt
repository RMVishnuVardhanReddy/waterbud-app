package com.waterbud.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.waterbud.app.MainActivity
import com.waterbud.app.R
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.WaterRepository
import com.waterbud.app.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget: shows today's progress and a one-tap "+250 ml" button.
 * Widgets are a top retention lever for habit apps.
 */
class WaterWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        scope.launch { render(context) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD) {
            val pending = goAsync()
            scope.launch {
                try {
                    WaterRepository(context).addDrink(250, "water")
                    ReminderScheduler.reschedule(context)
                    render(context)
                } finally { pending.finish() }
            }
        }
    }

    companion object {
        const val ACTION_ADD = "com.waterbud.app.WIDGET_ADD"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** Call after any logging action so the widget stays in sync. */
        fun updateAll(context: Context) { scope.launch { render(context) } }

        private suspend fun render(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, WaterWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val total = WaterRepository(context).totalForDay(Dates.todayKey()).first()
            val goal = PrefsRepository(context).prefs.first().effectiveGoalMl
            val pct = if (goal <= 0) 0 else (total * 100 / goal)

            val openPi = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val addPi = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, WaterWidgetProvider::class.java).setAction(ACTION_ADD),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.water_widget).apply {
                setTextViewText(R.id.widget_total, "$total / $goal ml")
                setTextViewText(R.id.widget_pct, "$pct%")
                setProgressBar(R.id.widget_progress, goal.coerceAtLeast(1), total.coerceAtMost(goal), false)
                setOnClickPendingIntent(R.id.widget_root, openPi)
                setOnClickPendingIntent(R.id.widget_add, addPi)
            }
            ids.forEach { mgr.updateAppWidget(it, views) }
        }
    }
}
