package com.waterbud.app

import android.app.Application
import com.waterbud.app.ads.AdManager
import com.waterbud.app.data.WaterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WaterbudApp : Application() {
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AdManager.init(this)
        appScope.launch { WaterRepository(this@WaterbudApp).ensureDefaultCups() }
    }
}
