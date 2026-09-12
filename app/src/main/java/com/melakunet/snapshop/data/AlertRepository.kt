package com.melakunet.snapshop.data

import com.melakunet.snapshop.network.BackendClient
import kotlinx.coroutines.flow.first

class AlertRepository(private val dao: SnapShopDao) {

    suspend fun checkAllAlerts(): Int {
        val alerts = dao.getAllPriceAlerts().first()
        var newlyFiredCount = 0
        val now = System.currentTimeMillis()

        for (alert in alerts) {
            try {
                val results = BackendClient.shop(alert.searchQuery)
                val lowestPrice = results.minOfOrNull { it.extractedPrice } ?: continue
                
                val wasTriggered = alert.triggered
                val isTriggered = lowestPrice <= alert.targetPrice
                
                dao.updatePriceAlertStatus(alert.id, isTriggered, now)
                
                // Update the linked SavedItem's currentLowestPrice
                if (alert.savedItemId.isNotEmpty()) {
                    dao.updateSavedItemPrice(alert.savedItemId, lowestPrice)
                }

                if (isTriggered && !wasTriggered) {
                    newlyFiredCount++
                }
            } catch (e: Exception) {
                // Log and continue to next alert
                android.util.Log.e("AlertRepository", "Failed to check alert: ${alert.productName}", e)
            }
        }
        return newlyFiredCount
    }
}
