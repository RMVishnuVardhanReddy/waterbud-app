package com.waterbud.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * GDPR / consent gate (Google User Messaging Platform).
 *
 * Required before serving ads to users in the EEA/UK — skipping it risks the
 * AdMob account. For users where consent isn't required, [canRequestAds] is
 * simply true and nothing is shown.
 *
 * Flow: on launch we update consent info and show the consent form if required,
 * then the app only requests ads when [canRequestAds] is true.
 */
object ConsentManager {

    /**
     * Updates consent and shows the form if the user must choose. Calls [onReady]
     * when done (whether or not a form was shown, or on error — ads are then gated
     * by [canRequestAds]).
     */
    fun ensureConsent(activity: Activity, onReady: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity, params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ -> onReady() }
            },
            { _ -> onReady() } // on failure, proceed; canRequestAds stays false until resolved
        )
    }

    /** True when ads may be requested (consent given, or not required). */
    fun canRequestAds(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context).canRequestAds()
}
