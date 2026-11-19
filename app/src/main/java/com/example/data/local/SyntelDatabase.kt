package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedRequestEntity::class,
        CachedReceivedItemEntity::class,
        CachedProductEntity::class,
        CachedWarehouseCardEntity::class,
        CachedDraftItemEntity::class,
        CachedStockItemEntity::class,
        CachedVoucherEntity::class,
        CachedPurchaseRowEntity::class,
        CachedPurchaseHistoryEntity::class,
        SyncMetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SyntelDatabase : RoomDatabase() {
    abstract fun syntelDao(): SyntelDao

    companion object {
        @Volatile
        private var instance: SyntelDatabase? = null

        fun getInstance(context: Context): SyntelDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SyntelDatabase::class.java,
                    "syntel_offline_cache.db",
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
