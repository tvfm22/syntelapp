package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SyntelDao {

    // ── Requester Requests ──
    @Query("SELECT * FROM cached_requests ORDER BY sortOrder ASC")
    fun observeRequests(): Flow<List<CachedRequestEntity>>

    @Query("DELETE FROM cached_requests")
    suspend fun clearRequests()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(items: List<CachedRequestEntity>)

    @Transaction
    suspend fun replaceRequests(items: List<CachedRequestEntity>) {
        clearRequests()
        insertRequests(items)
    }

    // ── Received Items ──
    @Query("SELECT * FROM cached_received_items ORDER BY isConfirmed ASC, sortOrder ASC")
    fun observeReceivedItems(): Flow<List<CachedReceivedItemEntity>>

    @Query("DELETE FROM cached_received_items")
    suspend fun clearReceivedItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivedItems(items: List<CachedReceivedItemEntity>)

    @Transaction
    suspend fun replaceReceivedItems(items: List<CachedReceivedItemEntity>) {
        clearReceivedItems()
        insertReceivedItems(items)
    }

    // ── Products Catalog ──
    @Query("SELECT * FROM cached_products ORDER BY name ASC")
    fun observeProducts(): Flow<List<CachedProductEntity>>

    @Query("DELETE FROM cached_products")
    suspend fun clearProducts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(items: List<CachedProductEntity>)

    @Transaction
    suspend fun replaceProducts(items: List<CachedProductEntity>) {
        clearProducts()
        insertProducts(items)
    }

    // ── Warehouse Officer Queue Cards ──
    @Query("SELECT * FROM cached_warehouse_cards ORDER BY sortOrder ASC")
    fun observeWarehouseCards(): Flow<List<CachedWarehouseCardEntity>>

    @Query("DELETE FROM cached_warehouse_cards")
    suspend fun clearWarehouseCards()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouseCards(items: List<CachedWarehouseCardEntity>)

    @Transaction
    suspend fun replaceWarehouseCards(items: List<CachedWarehouseCardEntity>) {
        clearWarehouseCards()
        insertWarehouseCards(items)
    }

    // ── Warehouse Purchase Draft ──
    @Query("SELECT * FROM cached_draft_items ORDER BY id ASC")
    fun observeDraftItems(): Flow<List<CachedDraftItemEntity>>

    @Query("DELETE FROM cached_draft_items")
    suspend fun clearDraftItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraftItems(items: List<CachedDraftItemEntity>)

    @Transaction
    suspend fun replaceDraftItems(items: List<CachedDraftItemEntity>) {
        clearDraftItems()
        insertDraftItems(items)
    }

    // ── Warehouse Stock Items ──
    @Query("SELECT * FROM cached_stock_items ORDER BY warehouseName ASC, productName ASC")
    fun observeStockItems(): Flow<List<CachedStockItemEntity>>

    @Query("DELETE FROM cached_stock_items")
    suspend fun clearStockItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockItems(items: List<CachedStockItemEntity>)

    @Transaction
    suspend fun replaceStockItems(items: List<CachedStockItemEntity>) {
        clearStockItems()
        insertStockItems(items)
    }

    // ── Warehouse Vouchers ──
    @Query("SELECT * FROM cached_vouchers ORDER BY id DESC")
    fun observeVouchers(): Flow<List<CachedVoucherEntity>>

    @Query("DELETE FROM cached_vouchers")
    suspend fun clearVouchers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVouchers(items: List<CachedVoucherEntity>)

    @Transaction
    suspend fun replaceVouchers(items: List<CachedVoucherEntity>) {
        clearVouchers()
        insertVouchers(items)
    }

    // ── Purchasing Dashboard Rows ──
    @Query("SELECT * FROM cached_purchase_rows WHERE scope = :scope ORDER BY sortOrder ASC")
    fun observePurchaseRows(scope: String): Flow<List<CachedPurchaseRowEntity>>

    @Query("DELETE FROM cached_purchase_rows WHERE scope = :scope")
    suspend fun clearPurchaseRows(scope: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseRows(items: List<CachedPurchaseRowEntity>)

    @Transaction
    suspend fun replacePurchaseRows(scope: String, items: List<CachedPurchaseRowEntity>) {
        clearPurchaseRows(scope)
        insertPurchaseRows(items)
    }

    // ── Purchasing History ──
    @Query("SELECT * FROM cached_purchase_history WHERE scope = :scope ORDER BY sortOrder ASC")
    fun observePurchaseHistory(scope: String): Flow<List<CachedPurchaseHistoryEntity>>

    @Query("DELETE FROM cached_purchase_history WHERE scope = :scope")
    suspend fun clearPurchaseHistory(scope: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseHistory(items: List<CachedPurchaseHistoryEntity>)

    @Transaction
    suspend fun replacePurchaseHistory(scope: String, items: List<CachedPurchaseHistoryEntity>) {
        clearPurchaseHistory(scope)
        insertPurchaseHistory(items)
    }

    // ── Sync Metadata ──
    @Query("SELECT * FROM sync_metadata WHERE resourceKey = :key LIMIT 1")
    fun observeSyncMeta(key: String): Flow<SyncMetaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMeta(meta: SyncMetaEntity)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearSyncMeta()

    @Query("DELETE FROM cached_purchase_rows")
    suspend fun clearAllPurchaseRows()

    @Query("DELETE FROM cached_purchase_history")
    suspend fun clearAllPurchaseHistory()

    @Transaction
    suspend fun clearAllCache() {
        clearRequests()
        clearReceivedItems()
        clearProducts()
        clearWarehouseCards()
        clearDraftItems()
        clearStockItems()
        clearVouchers()
        clearAllPurchaseRows()
        clearAllPurchaseHistory()
        clearSyncMeta()
    }
}
