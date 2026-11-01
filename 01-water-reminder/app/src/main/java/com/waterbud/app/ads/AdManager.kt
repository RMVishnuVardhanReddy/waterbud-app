package com.waterbud.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * AdMob wrapper tuned for "earn without irritating", based on monetization research:
 *
 *  • Banner — baseline revenue, only on secondary screens (History/Settings), never Home.
 *  • Interstitial — ONLY at a natural break (leaving a detail screen back to Home),
 *      hard-capped per day, with a minimum gap, and NEVER on app launch or on the
 *      "log water" action. (Launch interstitials are the #1 retention killer.)
 *  • Rewarded — always opt-in: the user taps "watch an ad" to unlock a perk
 *      (e.g. a premium theme for the day). Highest eCPM, lowest annoyance.
 *
 * All IDs below are Google's PUBLIC TEST IDs. Replace before release.
 */
object AdManager {

    const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_TEST = "ca-app-pub-3940256099942544/5224354917"

    // --- Frequency cap tuning (conservative on purpose) ---
    private const val MAX_INTERSTITIALS_PER_DAY = 3
    private const val MIN_GAP_MS = 3 * 60 * 1000L      // ≥ 3 minutes between interstitials
    private const val GRACE_AFTER_LAUNCH_MS = 90 * 1000L // no interstitial in first 90s

    private var interstitial: InterstitialAd? = null
    private var rewarded: RewardedAd? = null
    @Volatile private var initialized = false
    private val sessionStart = System.currentTimeMillis()

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        MobileAds.initialize(context) {}
    }

    // ---------------- Interstitial (capped, at breaks only) ----------------

    fun preloadInterstitial(context: Context) {
        if (interstitial != null) return
        InterstitialAd.load(
            context, INTERSTITIAL_TEST, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { interstitial = null }
            }
        )
    }

    /**
     * Call this at a natural transition (e.g. returning Home from History/Settings).
     * It self-limits via the frequency cap; most transitions will NOT show an ad.
     */
    fun maybeShowAtBreak(activity: Activity) {
        val ad = interstitial ?: return
        val cap = AdFrequency(activity)
        val now = System.currentTimeMillis()

        if (now - sessionStart < GRACE_AFTER_LAUNCH_MS) return
        if (cap.countToday() >= MAX_INTERSTITIALS_PER_DAY) return
        if (now - cap.lastShownMs() < MIN_GAP_MS) return

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            // Count a slot only when the ad actually displays, not when a show fails.
            override fun onAdShowedFullScreenContent() { cap.record(System.currentTimeMillis()) }
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadInterstitial(activity)
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { interstitial = null }
        }
        ad.show(activity)
    }

    // ---------------- Rewarded (opt-in only) ----------------

    fun preloadRewarded(context: Context) {
        if (rewarded != null) return
        RewardedAd.load(
            context, REWARDED_TEST, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewarded = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null }
            }
        )
    }

    val isRewardedReady: Boolean get() = rewarded != null

    /** [onReward] runs only if the user watches to the end. [onUnavailable] if no ad is ready. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewarded
        if (ad == null) { onUnavailable(); preloadRewarded(activity); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewarded = null; preloadRewarded(activity) }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { rewarded = null }
        }
        ad.show(activity, OnUserEarnedRewardListener { onReward() })
    }
}
