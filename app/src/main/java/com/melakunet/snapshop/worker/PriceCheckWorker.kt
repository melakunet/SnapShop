package com.melakunet.snapshop.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.melakunet.snapshop.R
import com.melakunet.snapshop.SnapShopApplication
import com.melakunet.snapshop.data.AlertRepository

class PriceCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = (applicationContext as SnapShopApplication).database
        val repository = AlertRepository(database.dao())

        val newlyFired = repository.checkAllAlerts()
        if (newlyFired > 0) {
            sendNotification(newlyFired)
        }

        return Result.success()
    }

    private fun sendNotification(count: Int) {
        val channelId = "price_alerts"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Price Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle("Price Drops Found!")
            .setContentText("$count of your tracked items have reached their target price.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }
}
