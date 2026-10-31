package com.waterbud.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A Compose-friendly AdMob banner. Hidden entirely for premium users, and it only
 * requests an ad once GDPR consent allows it (ConsentManager.canRequestAds).
 */
@Composable
fun BannerAd(isPremium: Boolean, modifier: Modifier = Modifier) {
    if (isPremium) return
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdManager.BANNER_TEST
                if (ConsentManager.canRequestAds(ctx)) loadAd(AdRequest.Builder().build())
            }
        }
    )
}
