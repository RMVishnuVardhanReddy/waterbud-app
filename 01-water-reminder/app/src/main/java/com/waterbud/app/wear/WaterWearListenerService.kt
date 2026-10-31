package com.waterbud.app.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.waterbud.app.data.WaterRepository
import com.waterbud.app.reminder.ReminderScheduler
import com.waterbud.app.widget.WaterWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives "log water" messages from the paired watch, logs them to the phone's
 * database (the single source of truth), then updates reminders, the widget, and
 * pushes the new total back to the watch.
 */
class WaterWearListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == PATH_LOG) {
            val ml = String(event.data).toIntOrNull()?.coerceIn(1, 5000) ?: 250
            scope.launch {
                WaterRepository(applicationContext).addDrink(ml, "water")
                ReminderScheduler.reschedule(applicationContext)
                WaterWidgetProvider.updateAll(applicationContext)
                WearSync.pushToday(applicationContext)
            }
        }
    }

    companion object { const val PATH_LOG = "/log_water" }
}
