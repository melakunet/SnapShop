package com.melakunet.snapshop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScanRecord::class, SavedItem::class, PriceAlert::class], version = 1, exportSchema = false)
abstract class SnapShopDatabase : RoomDatabase() {
    abstract fun dao(): SnapShopDao

    companion object {
        @Volatile
        private var INSTANCE: SnapShopDatabase? = null

        fun getDatabase(context: Context): SnapShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SnapShopDatabase::class.java,
                    "snapshop_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
