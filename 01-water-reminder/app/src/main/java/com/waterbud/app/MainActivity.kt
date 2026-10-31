package com.waterbud.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.waterbud.app.ads.AdManager
import com.waterbud.app.ads.ConsentManager
import com.waterbud.app.billing.BillingManager
import com.waterbud.app.data.Dates
import com.waterbud.app.data.PrefsRepository
import com.waterbud.app.data.ThemeMode
import com.waterbud.app.data.WeatherManager
import com.waterbud.app.health.HealthConnect
import com.waterbud.app.reminder.ReminderScheduler
import com.waterbud.app.widget.WaterWidgetProvider
import com.waterbud.app.ui.navigation.AppNav
import com.waterbud.app.ui.theme.WaterbudTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var billing: BillingManager
    private lateinit var prefsRepo: PrefsRepository

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    // Health Connect permission request (write hydration).
    private var hcResultCallback: ((Boolean) -> Unit)? = null
    private val hcPermission =
        registerForActivityResult(HealthConnect.permissionContract()) { granted ->
            val ok = granted.containsAll(HealthConnect.PERMISSIONS)
            if (ok) lifecycleScope.launch { prefsRepo.setHealthSyncEnabled(true) }
            hcResultCallback?.invoke(ok); hcResultCallback = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsRepo = PrefsRepository(applicationContext)
        billing = BillingManager(applicationContext) { /* premium flips via DataStore flow */ }
        billing.start()

        // Gather GDPR consent first; only request ads once allowed. We still never
        // show an interstitial on launch (that only hurts retention).
        ConsentManager.ensureConsent(this) {
            if (ConsentManager.canRequestAds(this)) {
                AdManager.preloadInterstitial(this)
                AdManager.preloadRewarded(this)
            }
        }
        askNotificationPermission()

        // Re-arm reminders on every launch so they survive app updates / lost alarms,
        // and refresh the weather-adjusted goal (no-op unless the user enabled it).
        lifecycleScope.launch {
            ReminderScheduler.reschedule(applicationContext)
            WeatherManager.refreshIfEnabled(applicationContext)
            WaterWidgetProvider.updateAll(applicationContext)
        }

        setContent {
            val prefs by prefsRepo.prefs.collectAsState(initial = null)
            val p = prefs

            WaterbudTheme(themeMode = p?.theme ?: ThemeMode.SYSTEM) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (p == null) {
                        LoadingBox()
                    } else {
                        AppNav(
                            startOnboarding = !p.onboarded,
                            onUpgrade = { if (!p.isPremium) billing.launchPurchase(this) },
                            // Natural-break interstitial, frequency-capped; no-op if premium.
                            onLeaveDetail = { if (!p.isPremium) AdManager.maybeShowAtBreak(this) },
                            onWatchRewardedForThemes = { onDone ->
                                AdManager.showRewarded(
                                    this,
                                    onReward = {
                                        lifecycleScope.launch {
                                            prefsRepo.setRewardThemesUntil(Dates.endOfTodayMillis())
                                            onDone(true)
                                        }
                                    },
                                    onUnavailable = { onDone(false) }
                                )
                            },
                            onEnableHealthSync = { onResult ->
                                if (!HealthConnect.isAvailable(this)) onResult(false)
                                else { hcResultCallback = onResult; hcPermission.launch(HealthConnect.PERMISSIONS) }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@androidx.compose.runtime.Composable
private fun LoadingBox() {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) { CircularProgressIndicator() }
}
