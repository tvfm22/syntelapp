package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ── Auth ─────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val username: String,
    val password: String,
    @Json(name = "cafe_code") val cafeCode: String = "",
)

@JsonClass(generateAdapter = true)
data class CafeOptionDto(
    val id: Int,
    val code: String,
    val name: String,
)

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    val id: Int,
    val username: String,
    @Json(name = "full_name") val fullName: String,
    val role: String,
    @Json(name = "role_display") val roleDisplay: String,
    @Json(name = "secondary_role") val secondaryRole: String = "",
    @Json(name = "is_requester") val isRequester: Boolean = false,
    @Json(name = "has_warehouse_role") val hasWarehouseRole: Boolean = false,
    @Json(name = "has_purchasing_role") val hasPurchasingRole: Boolean = false,
    @Json(name = "has_dual_role") val hasDualRole: Boolean = false,
    @Json(name = "department_name") val departmentName: String = "",
    @Json(name = "cafe_code") val cafeCode: String = "",
    @Json(name = "cafe_name") val cafeName: String = "",
    @Json(name = "needs_cafe_selection") val needsCafeSelection: Boolean = false,
    @Json(name = "available_cafes") val availableCafes: List<CafeOptionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    val token: String,
    val user: UserProfileDto,
)

@JsonClass(generateAdapter = true)
data class MeResponseDto(
    val user: UserProfileDto,
)

@JsonClass(generateAdapter = true)
data class MessageResponseDto(
    val detail: String = "",
    val id: Int? = null,
    @Json(name = "request_number") val requestNumber: String? = null,
    @Json(name = "voucher_number") val voucherNumber: String? = null,
    @Json(name = "confirmed_count") val confirmedCount: Int? = null,
    @Json(name = "is_self_buy") val isSelfBuy: Boolean? = null,
    @Json(name = "direct_count") val directCount: Int? = null,
    @Json(name = "ceo_count") val ceoCount: Int? = null,
)

// ── Requester ────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RequesterSummaryDto(
    @Json(name = "total_requests") val totalRequests: Int = 0,
    @Json(name = "pending_requests") val pendingRequests: Int = 0,
    @Json(name = "completed_requests") val completedRequests: Int = 0,
    @Json(name = "received_pending_count") val receivedPendingCount: Int = 0,
    @Json(name = "receipt_gate_blocked") val receiptGateBlocked: Boolean = false,
    @Json(name = "receipt_gate_message") val receiptGateMessage: String = "",
)

@JsonClass(generateAdapter = true)
data class ProductCatalogDto(
    val id: Int,
    val name: String,
    @Json(name = "unit_name") val unitName: String = "عدد",
    @Json(name = "category_name") val categoryName: String = "عمومی",
    @Json(name = "current_stock") val currentStock: Int = 0,
    @Json(name = "is_blocked") val isBlocked: Boolean = false,
    @Json(name = "blocked_reason") val blockedReason: String = "",
)

@JsonClass(generateAdapter = true)
data class ProductsCatalogResponseDto(
    val products: List<ProductCatalogDto> = emptyList(),
    val units: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RequestItemDto(
    val id: Int = 0,
    @Json(name = "product_id") val productId: Int? = null,
    val name: String,
    val quantity: String,
    val unit: String = "عدد",
    val description: String = "",
    @Json(name = "supply_status") val supplyStatus: String = "pending",
    @Json(name = "supply_status_label") val supplyStatusLabel: String = "در انتظار",
    @Json(name = "purchased_quantity") val purchasedQuantity: String = "",
)

@JsonClass(generateAdapter = true)
data class PurchaseRequestDto(
    val id: Int,
    @Json(name = "request_number") val requestNumber: String,
    val status: String,
    @Json(name = "status_label") val statusLabel: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "created_at_jalali") val createdAtJalali: String = "",
    @Json(name = "created_date_iso") val createdDateIso: String = "",
    @Json(name = "delivery_date") val deliveryDate: String = "",
    @Json(name = "delivery_date_jalali") val deliveryDateJalali: String = "",
    @Json(name = "delivery_immediate") val deliveryImmediate: Boolean = false,
    @Json(name = "rejection_reason") val rejectionReason: String = "",
    @Json(name = "can_edit") val canEdit: Boolean = false,
    val items: List<RequestItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RequesterRequestsResponseDto(
    val requests: List<PurchaseRequestDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CreateRequestItemPayload(
    @Json(name = "product_id") val productId: Int? = null,
    val name: String,
    val quantity: String,
    val unit: String,
    val description: String = "",
)

@JsonClass(generateAdapter = true)
data class CreateRequestPayload(
    @Json(name = "delivery_date") val deliveryDate: String = "",
    @Json(name = "delivery_immediate") val deliveryImmediate: Boolean = false,
    val items: List<CreateRequestItemPayload>,
)

@JsonClass(generateAdapter = true)
data class ReceivedDeliveryPartDto(
    val qty: String,
    val unit: String,
    val warehouse: String = "",
)

@JsonClass(generateAdapter = true)
data class ReceivedItemDto(
    @Json(name = "item_id") val itemId: Int,
    @Json(name = "request_number") val requestNumber: String,
    val name: String,
    @Json(name = "requested_qty") val requestedQty: String,
    val unit: String,
    val deliveries: List<ReceivedDeliveryPartDto> = emptyList(),
    @Json(name = "is_confirmed") val isConfirmed: Boolean = false,
    @Json(name = "confirmed_at_jalali") val confirmedAtJalali: String = "",
)

@JsonClass(generateAdapter = true)
data class ReceivedItemsResponseDto(
    @Json(name = "pending_count") val pendingCount: Int = 0,
    val pending: List<ReceivedItemDto> = emptyList(),
    val confirmed: List<ReceivedItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ConfirmReceivedPayload(
    val action: String,
    @Json(name = "item_id") val itemId: Int? = null,
)

// ── Warehouse Manager / Officer ──────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class WarehouseStatsDto(
    val pending: Int = 0,
    val done: Int = 0,
    val enough: Int = 0,
    val short: Int = 0,
    @Json(name = "no_stock") val noStock: Int = 0,
)

@JsonClass(generateAdapter = true)
data class StockOptionDto(
    @Json(name = "stock_item_id") val stockItemId: Int,
    @Json(name = "warehouse_name") val warehouseName: String,
    @Json(name = "available_qty") val availableQty: String,
    @Json(name = "unit_label") val unitLabel: String = "",
)

@JsonClass(generateAdapter = true)
data class WarehouseVisibleItemDto(
    @Json(name = "item_id") val itemId: Int,
    @Json(name = "product_id") val productId: Int? = null,
    val name: String,
    val quantity: String,
    val unit: String = "عدد",
    val description: String = "",
    @Json(name = "available_qty") val availableQty: String = "0",
    @Json(name = "has_stock") val hasStock: Boolean = false,
    @Json(name = "is_enough") val isEnough: Boolean = false,
    @Json(name = "stock_options") val stockOptions: List<StockOptionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WarehouseDoneItemDto(
    @Json(name = "item_id") val itemId: Int,
    val name: String,
    val quantity: String,
    val unit: String = "عدد",
    val status: String,
    @Json(name = "status_label") val statusLabel: String,
    @Json(name = "supplied_qty") val suppliedQty: String = "",
    val notes: String = "",
    @Json(name = "is_locked") val isLocked: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class WarehouseRequestCardDto(
    @Json(name = "request_id") val requestId: Int,
    @Json(name = "request_number") val requestNumber: String,
    @Json(name = "requester_name") val requesterName: String,
    @Json(name = "department_name") val departmentName: String,
    @Json(name = "created_at_jalali") val createdAtJalali: String = "",
    @Json(name = "delivery_date_jalali") val deliveryDateJalali: String = "",
    @Json(name = "delivery_immediate") val deliveryImmediate: Boolean = false,
    @Json(name = "visible_items") val visibleItems: List<WarehouseVisibleItemDto> = emptyList(),
    @Json(name = "done_items") val doneItems: List<WarehouseDoneItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class WarehouseDashboardResponseDto(
    @Json(name = "selected_date") val selectedDate: String = "",
    @Json(name = "selected_date_jalali") val selectedDateJalali: String = "",
    @Json(name = "today_iso") val todayIso: String = "",
    val stats: WarehouseStatsDto = WarehouseStatsDto(),
    val departments: List<String> = emptyList(),
    @Json(name = "draft_count") val draftCount: Int = 0,
    @Json(name = "my_purchases_count") val myPurchasesCount: Int = 0,
    val cards: List<WarehouseRequestCardDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class StockAllocationPayload(
    @Json(name = "stock_item_id") val stockItemId: Int,
    val quantity: String,
)

@JsonClass(generateAdapter = true)
data class WarehouseItemActionPayload(
    val action: String,
    val allocations: List<StockAllocationPayload> = emptyList(),
    val reason: String = "",
)

@JsonClass(generateAdapter = true)
data class DraftItemDto(
    val id: Int,
    @Json(name = "product_id") val productId: Int? = null,
    val name: String,
    val quantity: String,
    val unit: String = "عدد",
    val description: String = "",
    @Json(name = "needs_ceo") val needsCeo: Boolean = false,
    @Json(name = "ceo_reasons") val ceoReasons: List<String> = emptyList(),
    @Json(name = "from_request") val fromRequest: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class OfficerOptionDto(
    val id: String,
    val name: String,
)

@JsonClass(generateAdapter = true)
data class WarehouseDraftResponseDto(
    val items: List<DraftItemDto> = emptyList(),
    val officers: List<OfficerOptionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class AddDraftItemPayload(
    @Json(name = "product_id") val productId: Int? = null,
    val name: String,
    val quantity: String,
    val unit: String = "عدد",
    val description: String = "",
)

@JsonClass(generateAdapter = true)
data class UpdateDraftItemPayload(
    val quantity: String,
    val description: String = "",
)

@JsonClass(generateAdapter = true)
data class SendDraftPayload(
    @Json(name = "selected_item_ids") val selectedItemIds: List<Int>,
    @Json(name = "officer_user_id") val officerUserId: String,
)

@JsonClass(generateAdapter = true)
data class WarehouseOptionDto(
    val id: Int,
    val name: String,
    val code: String = "",
    val location: String = "",
)

@JsonClass(generateAdapter = true)
data class StockItemDto(
    val id: Int,
    @Json(name = "product_id") val productId: Int,
    @Json(name = "product_name") val productName: String,
    @Json(name = "category_name") val categoryName: String = "عمومی",
    @Json(name = "warehouse_id") val warehouseId: Int = 0,
    @Json(name = "warehouse_name") val warehouseName: String = "",
    val quantity: String,
    @Json(name = "min_quantity") val minQuantity: String = "0",
    @Json(name = "unit_name") val unitName: String = "عدد",
    @Json(name = "is_low_stock") val isLowStock: Boolean = false,
    @Json(name = "last_updated_jalali") val lastUpdatedJalali: String = "",
)

@JsonClass(generateAdapter = true)
data class WarehouseStockResponseDto(
    val warehouses: List<WarehouseOptionDto> = emptyList(),
    val items: List<StockItemDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class StockTransactionDto(
    val id: Int,
    val type: String,
    @Json(name = "type_label") val typeLabel: String,
    val quantity: String,
    @Json(name = "previous_quantity") val previousQuantity: String,
    @Json(name = "new_quantity") val newQuantity: String,
    val description: String = "",
    @Json(name = "created_by") val createdBy: String = "",
    @Json(name = "created_at_jalali") val createdAtJalali: String = "",
)

@JsonClass(generateAdapter = true)
data class StockTransactionsResponseDto(
    @Json(name = "product_name") val productName: String = "",
    @Json(name = "warehouse_name") val warehouseName: String = "",
    @Json(name = "current_quantity") val currentQuantity: String = "0",
    val transactions: List<StockTransactionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class VoucherDto(
    val id: Int,
    @Json(name = "voucher_number") val voucherNumber: String,
    @Json(name = "voucher_type") val voucherType: String,
    @Json(name = "voucher_type_label") val voucherTypeLabel: String,
    val status: String,
    @Json(name = "status_label") val statusLabel: String,
    @Json(name = "source_warehouse") val sourceWarehouse: String = "",
    @Json(name = "destination_warehouse") val destinationWarehouse: String = "",
    val description: String = "",
    @Json(name = "date_jalali") val dateJalali: String = "",
    @Json(name = "item_count") val itemCount: Int = 0,
    @Json(name = "items_summary") val itemsSummary: String = "",
)

@JsonClass(generateAdapter = true)
data class VouchersResponseDto(
    val vouchers: List<VoucherDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class CreateVoucherItemPayload(
    @Json(name = "product_id") val productId: Int,
    val quantity: String,
    val description: String = "",
)

@JsonClass(generateAdapter = true)
data class CreateVoucherPayload(
    @Json(name = "voucher_type") val voucherType: String,
    @Json(name = "source_warehouse_id") val sourceWarehouseId: Int? = null,
    @Json(name = "destination_warehouse_id") val destinationWarehouseId: Int? = null,
    val description: String = "",
    val items: List<CreateVoucherItemPayload>,
)

// ── Purchasing Officer & Warehouse Self-Buy ──────────────────────────────────

@JsonClass(generateAdapter = true)
data class SectionQtyDto(
    val name: String,
    val qty: String,
)

@JsonClass(generateAdapter = true)
data class PurchaseRowDto(
    @Json(name = "row_key") val rowKey: String,
    val name: String,
    val unit: String = "عدد",
    @Json(name = "total_qty") val totalQty: String,
    val sections: List<SectionQtyDto> = emptyList(),
    @Json(name = "assignment_ids") val assignmentIds: List<Int> = emptyList(),
    val descriptions: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PurchasingDashboardResponseDto(
    val scope: String = "purchasing",
    @Json(name = "total_items") val totalItems: Int = 0,
    @Json(name = "awaiting_ceo_count") val awaitingCeoCount: Int = 0,
    val rows: List<PurchaseRowDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PurchasingActionPayload(
    val action: String,
    @Json(name = "assignment_ids") val assignmentIds: List<Int>,
    val scope: String = "purchasing",
    val notes: String = "",
)

@JsonClass(generateAdapter = true)
data class PurchaseHistoryItemDto(
    @Json(name = "assignment_id") val assignmentId: Int,
    val name: String,
    val quantity: String,
    val unit: String = "عدد",
    @Json(name = "department_name") val departmentName: String = "",
    @Json(name = "request_number") val requestNumber: String = "",
    val status: String,
    @Json(name = "status_label") val statusLabel: String,
    @Json(name = "date_jalali") val dateJalali: String = "",
    @Json(name = "time_str") val timeStr: String = "",
    val notes: String = "",
)

@JsonClass(generateAdapter = true)
data class PurchasingHistoryResponseDto(
    val scope: String = "purchasing",
    val items: List<PurchaseHistoryItemDto> = emptyList(),
)
