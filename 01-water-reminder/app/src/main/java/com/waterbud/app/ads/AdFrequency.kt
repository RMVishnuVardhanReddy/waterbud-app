package com.waterbud.app.ads

import android.content.Context
import com.waterbud.app.data.Dates

/**
 * Tiny synchronous SharedPreferences store for the interstitial frequency cap.
 * Keeps a per-day count and the last-shown timestamp so ads stay rare.
 */
class AdFrequency(context: Context) {
    private val sp = context.getSharedPreferences("ad_cap", Context.MODE_PRIVATE)

    fun countToday(): Int {
        return if (sp.getString(KEY_DAY, "") == Dates.todayKey()) sp.getInt(KEY_COUNT, 0) else 0
    }

    fun lastShownMs(): Long = sp.getLong(KEY_LAST, 0L)

    fun record(nowMs: Long) {
        val today = Dates.todayKey()
        val count = if (sp.getString(KEY_DAY, "") == today) sp.getInt(KEY_COUNT, 0) + 1 else 1
        sp.edit().putString(KEY_DAY, today).putInt(KEY_COUNT, count).putLong(KEY_LAST, nowMs).apply()
    }

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_COUNT = "count"
        const val KEY_LAST = "last"
    }
}
