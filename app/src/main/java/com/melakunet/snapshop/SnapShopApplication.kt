package com.melakunet.snapshop

import android.app.Application
import androidx.work.*
import com.melakunet.snapshop.data.SnapShopDatabase
import com.melakunet.snapshop.worker.PriceCheckWorker
import java.util.concurrent.TimeUnit

class SnapShopApplication : Application() {
    val database: SnapShopDatabase by lazy { SnapShopDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        schedulePriceChecks()
    }

    private fun schedulePriceChecks() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "PriceCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
