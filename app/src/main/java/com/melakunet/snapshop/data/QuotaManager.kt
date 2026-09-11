package com.melakunet.snapshop.data

import android.content.Context
import android.content.SharedPreferences
import com.melakunet.snapshop.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class QuotaManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("quota_prefs", Context.MODE_PRIVATE)

    var isPro: Boolean
        get() = prefs.getBoolean("is_pro", false)
        set(value) = prefs.edit().putBoolean("is_pro", value).apply()

    val freeLimit: Int = if (BuildConfig.DEBUG) 300 else 10

    fun getUsedQuota(): Int {
        val currentMonth = getCurrentMonthKey()
        val lastMonth = prefs.getString("quotaMonth", "")
        if (currentMonth != lastMonth) {
            prefs.edit().putString("quotaMonth", currentMonth).putInt("scanCount", 0).apply()
            return 0
        }
        return prefs.getInt("scanCount", 0)
    }

    fun consumeQuota() {
        if (isPro) return
        val count = getUsedQuota()
        prefs.edit().putInt("scanCount", count + 1).apply()
    }

    fun canScan(): Boolean {
        if (isPro) return true
        return getUsedQuota() < freeLimit
    }

    private fun getCurrentMonthKey(): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }
}
