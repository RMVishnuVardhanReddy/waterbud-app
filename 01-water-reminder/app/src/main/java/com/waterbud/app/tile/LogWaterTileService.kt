package com.waterbud.app.tile

import android.service.quicksettings.TileService
import com.waterbud.app.data.WaterRepository
import com.waterbud.app.reminder.ReminderScheduler
import com.waterbud.app.widget.WaterWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Quick Settings tile — pull down the shade, tap once, log a 250 ml glass.
 * The fastest possible logging path; no app launch needed.
 */
class LogWaterTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onClick() {
        super.onClick()
        scope.launch {
            WaterRepository(applicationContext).addDrink(250, "water")
            ReminderScheduler.reschedule(applicationContext)
            WaterWidgetProvider.updateAll(applicationContext)
        }
    }
}
