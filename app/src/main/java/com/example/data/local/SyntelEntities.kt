package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_requests")
data class CachedRequestEntity(
    @PrimaryKey val id: Int,
    val requestNumber: String,
    val status: String,
    val statusLabel: String,
    val createdAt: String,
    val createdAtJalali: String,
    val createdDateIso: String,
    val deliveryDate: String,
    val deliveryDateJalali: String,
    val deliveryImmediate: Boolean,
    val rejectionReason: String,
    val canEdit: Boolean,
    val itemsJson: String,
    val sortOrder: Int,
)

@Entity(tableName = "cached_received_items")
data class CachedReceivedItemEntity(
    @PrimaryKey val itemId: Int,
    val requestNumber: String,
    val name: String,
    val requestedQty: String,
    val unit: String,
    val deliveriesJson: String,
    val isConfirmed: Boolean,
    val confirmedAtJalali: String,
    val sortOrder: Int,
)

@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val unitName: String,
    val categoryName: String,
    val currentStock: Int,
    val isBlocked: Boolean,
    val blockedReason: String,
)

@Entity(tableName = "cached_warehouse_cards")
data class CachedWarehouseCardEntity(
    @PrimaryKey val requestId: Int,
    val requestNumber: String,
    val requesterName: String,
    val departmentName: String,
    val createdAtJalali: String,
    val deliveryDateJalali: String,
    val deliveryImmediate: Boolean,
    val visibleItemsJson: String,
    val doneItemsJson: String,
    val selectedDateIso: String,
    val sortOrder: Int,
)

@Entity(tableName = "cached_draft_items")
data class CachedDraftItemEntity(
    @PrimaryKey val id: Int,
    val productId: Int?,
    val name: String,
    val quantity: String,
    val unit: String,
    val description: String,
    val needsCeo: Boolean,
    val ceoReasonsJson: String,
    val fromRequest: Boolean,
)

@Entity(tableName = "cached_stock_items")
data class CachedStockItemEntity(
    @PrimaryKey val id: Int,
    val productId: Int,
    val productName: String,
    val categoryName: String,
    val warehouseId: Int,
    val warehouseName: String,
    val quantity: String,
    val minQuantity: String,
    val unitName: String,
    val isLowStock: Boolean,
    val lastUpdatedJalali: String,
)

@Entity(tableName = "cached_vouchers")
data class CachedVoucherEntity(
    @PrimaryKey val id: Int,
    val voucherNumber: String,
    val voucherType: String,
    val voucherTypeLabel: String,
    val status: String,
    val statusLabel: String,
    val sourceWarehouse: String,
    val destinationWarehouse: String,
    val description: String,
    val dateJalali: String,
    val itemCount: Int,
    val itemsSummary: String,
)

@Entity(tableName = "cached_purchase_rows")
data class CachedPurchaseRowEntity(
    @PrimaryKey val cacheKey: String,
    val scope: String,
    val rowKey: String,
    val name: String,
    val unit: String,
    val totalQty: String,
    val sectionsJson: String,
    val assignmentIdsJson: String,
    val descriptionsJson: String,
    val sortOrder: Int,
)

@Entity(tableName = "cached_purchase_history")
data class CachedPurchaseHistoryEntity(
    @PrimaryKey val cacheKey: String,
    val scope: String,
    val assignmentId: Int,
    val name: String,
    val quantity: String,
    val unit: String,
    val departmentName: String,
    val requestNumber: String,
    val status: String,
    val statusLabel: String,
    val dateJalali: String,
    val timeStr: String,
    val notes: String,
    val sortOrder: Int,
)

@Entity(tableName = "sync_metadata")
data class SyncMetaEntity(
    @PrimaryKey val resourceKey: String,
    val lastSyncedMillis: Long,
    val extraPayloadJson: String = "",
)
