package com.waterbud.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.delay

/**
 * A Compose-friendly AdMob banner. Hidden for premium users, and it only loads once
 * GDPR consent allows ads. If consent resolves AFTER the first composition, we poll
 * briefly and show the banner then — so the slot doesn't stay permanently blank.
 */
@Composable
fun BannerAd(isPremium: Boolean, modifier: Modifier = Modifier) {
    if (isPremium) return
    val context = LocalContext.current
    var canAds by remember { mutableStateOf(ConsentManager.canRequestAds(context)) }

    LaunchedEffect(Unit) {
        var waited = 0
        while (!canAds && waited < 15_000) {
            delay(500); waited += 500
            canAds = ConsentManager.canRequestAds(context)
        }
    }

    if (!canAds) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdManager.BANNER_TEST
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
