package com.melakunet.snapshop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapShopDao {
    // Scan Records
    @Query("SELECT * FROM scan_records ORDER BY date DESC")
    fun getAllScanRecords(): Flow<List<ScanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanRecord(record: ScanRecord)

    @Delete
    suspend fun deleteScanRecord(record: ScanRecord)

    @Query("DELETE FROM scan_records")
    suspend fun clearAllScanRecords()

    // Saved Items
    @Query("SELECT * FROM saved_items ORDER BY savedDate DESC")
    fun getAllSavedItems(): Flow<List<SavedItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedItem(item: SavedItem)

    @Delete
    suspend fun deleteSavedItem(item: SavedItem)

    // Price Alerts
    @Query("SELECT * FROM price_alerts ORDER BY createdDate DESC")
    fun getAllPriceAlerts(): Flow<List<PriceAlert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceAlert(alert: PriceAlert)

    @Delete
    suspend fun deletePriceAlert(alert: PriceAlert)
}
