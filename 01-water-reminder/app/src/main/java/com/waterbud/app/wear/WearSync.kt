package com.waterbud.app.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.WaterRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/** Pushes today's total + goal to any paired watch via the Data Layer (best-effort). */
object WearSync {
    const val PATH_TODAY = "/today"

    suspend fun pushToday(context: Context) {
        runCatching {
            val total = WaterRepository(context).totalForDay(Dates.todayKey()).first()
            val goal = PrefsRepository(context).prefs.first().effectiveGoalMl
            val req = PutDataMapRequest.create(PATH_TODAY).apply {
                dataMap.putInt("total", total)
                dataMap.putInt("goal", goal)
                dataMap.putLong("ts", System.currentTimeMillis()) // force an update each change
            }
            Wearable.getDataClient(context).putDataItem(req.asPutDataRequest().setUrgent()).await()
        }
    }
}
