package com.melakunet.snapshop.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ScanRecord::class, SavedItem::class, PriceAlert::class, CachedPriceList::class],
    version = 2,
    exportSchema = true
)
abstract class SnapShopDatabase : RoomDatabase() {
    abstract fun dao(): SnapShopDao

    companion object {
        @Volatile
        private var INSTANCE: SnapShopDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `cached_prices` (`normalizedQuery` TEXT NOT NULL, `itemsJson` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`normalizedQuery`))")
            }
        }

        fun getDatabase(context: Context): SnapShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SnapShopDatabase::class.java,
                    "snapshop_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
