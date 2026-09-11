package com.melakunet.snapshop.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val productName: String,
    val mode: String,
    val thumbnail: ByteArray?,
    val lowestPrice: Double,
    val searchQuery: String
)

@Entity(tableName = "saved_items")
data class SavedItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productName: String,
    val searchQuery: String,
    val thumbnail: String?,
    val savedPrice: Double,
    val savedDate: Long,
    val link: String,
    val source: String,
    val currentLowestPrice: Double
)

@Entity(tableName = "price_alerts")
data class PriceAlert(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val savedItemId: String,
    val productName: String,
    val searchQuery: String,
    val targetPrice: Double,
    val createdDate: Long,
    val lastCheckedDate: Long,
    val triggered: Boolean
)
