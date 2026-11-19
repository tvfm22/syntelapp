package com.example.data.repository

import com.example.data.local.AppWorkspace
import com.example.data.local.CachedDraftItemEntity
import com.example.data.local.CachedProductEntity
import com.example.data.local.CachedPurchaseHistoryEntity
import com.example.data.local.CachedPurchaseRowEntity
import com.example.data.local.CachedReceivedItemEntity
import com.example.data.local.CachedRequestEntity
import com.example.data.local.CachedStockItemEntity
import com.example.data.local.CachedVoucherEntity
import com.example.data.local.CachedWarehouseCardEntity
import com.example.data.local.SessionStore
import com.example.data.local.SyncMetaEntity
import com.example.data.local.SyntelDao
import com.example.data.local.UserSession
import com.example.data.remote.AddDraftItemPayload
import com.example.data.remote.CafeOptionDto
import com.example.data.remote.ConfirmReceivedPayload
import com.example.data.remote.CreateRequestPayload
import com.example.data.remote.CreateVoucherPayload
import com.example.data.remote.DraftItemDto
import com.example.data.remote.LoginRequestDto
import com.example.data.remote.NetworkModule
import com.example.data.remote.OfficerOptionDto
import com.example.data.remote.ProductCatalogDto
import com.example.data.remote.PurchaseHistoryItemDto
import com.example.data.remote.PurchaseRequestDto
import com.example.data.remote.PurchaseRowDto
import com.example.data.remote.PurchasingActionPayload
import com.example.data.remote.ReceivedDeliveryPartDto
import com.example.data.remote.ReceivedItemDto
import com.example.data.remote.RequestItemDto
import com.example.data.remote.RequesterSummaryDto
import com.example.data.remote.SectionQtyDto
import com.example.data.remote.SendDraftPayload
import com.example.data.remote.StockAllocationPayload
import com.example.data.remote.StockItemDto
import com.example.data.remote.StockTransactionsResponseDto
import com.example.data.remote.SyntelApiService
import com.example.data.remote.UpdateDraftItemPayload
import com.example.data.remote.VoucherDto
import com.example.data.remote.WarehouseDoneItemDto
import com.example.data.remote.WarehouseItemActionPayload
import com.example.data.remote.WarehouseOptionDto
import com.example.data.remote.WarehouseRequestCardDto
import com.example.data.remote.WarehouseStatsDto
import com.example.data.remote.WarehouseVisibleItemDto
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@JsonClass(generateAdapter = true)
data class WarehouseDashboardMeta(
    val selectedDate: String = "",
    val selectedDateJalali: String = "",
    val todayIso: String = "",
    val stats: WarehouseStatsDto = WarehouseStatsDto(),
    val departments: List<String> = emptyList(),
    val draftCount: Int = 0,
    val myPurchasesCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class PurchasingMeta(
    val totalItems: Int = 0,
    val awaitingCeoCount: Int = 0,
)

class SyntelRepository(
    private val api: SyntelApiService,
    private val dao: SyntelDao,
    private val sessionStore: SessionStore,
) {
    private val moshi = NetworkModule.moshi

    private val requestItemsAdapter = moshi.adapter<List<RequestItemDto>>(
        Types.newParameterizedType(List::class.java, RequestItemDto::class.java)
    )
    private val deliveryPartsAdapter = moshi.adapter<List<ReceivedDeliveryPartDto>>(
        Types.newParameterizedType(List::class.java, ReceivedDeliveryPartDto::class.java)
    )
    private val visibleItemsAdapter = moshi.adapter<List<WarehouseVisibleItemDto>>(
        Types.newParameterizedType(List::class.java, WarehouseVisibleItemDto::class.java)
    )
    private val doneItemsAdapter = moshi.adapter<List<WarehouseDoneItemDto>>(
        Types.newParameterizedType(List::class.java, WarehouseDoneItemDto::class.java)
    )
    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val intListAdapter = moshi.adapter<List<Int>>(
        Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    )
    private val sectionListAdapter = moshi.adapter<List<SectionQtyDto>>(
        Types.newParameterizedType(List::class.java, SectionQtyDto::class.java)
    )
    private val officerListAdapter = moshi.adapter<List<OfficerOptionDto>>(
        Types.newParameterizedType(List::class.java, OfficerOptionDto::class.java)
    )
    private val warehouseListAdapter = moshi.adapter<List<WarehouseOptionDto>>(
        Types.newParameterizedType(List::class.java, WarehouseOptionDto::class.java)
    )
    private val requesterSummaryAdapter = moshi.adapter(RequesterSummaryDto::class.java)
    private val warehouseMetaAdapter = moshi.adapter(WarehouseDashboardMeta::class.java)
    private val purchasingMetaAdapter = moshi.adapter(PurchasingMeta::class.java)

    val sessionFlow: Flow<UserSession> = sessionStore.sessionFlow

    private suspend fun activeCafe(): String = sessionStore.sessionFlow.first().cafeCode

    // ── Auth ─────────────────────────────────────────────────────────────────

    suspend fun login(username: String, password: String, cafeCode: String): Result<UserSession> =
        runCatching {
            val response = api.login(LoginRequestDto(username.trim(), password, cafeCode.trim()))
            dao.clearAllCache()
            sessionStore.saveLogin(response.token, response.user)
            sessionStore.sessionFlow.first()
        }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun refreshProfile(): Result<Unit> = runCatching {
        val cafe = activeCafe()
        val res = api.getMe(cafe.ifBlank { null })
        sessionStore.updateProfile(res.user)
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun logout() {
        try {
            api.logout()
        } catch (_: Exception) {
            // Even if offline, clear local session and cache safely
        }
        dao.clearAllCache()
        sessionStore.clearSession()
    }

    suspend fun setWorkspace(workspace: AppWorkspace) = sessionStore.setActiveWorkspace(workspace)

    suspend fun setCafe(cafe: CafeOptionDto) {
        dao.clearAllCache()
        sessionStore.setActiveCafe(cafe)
    }

    suspend fun setDarkTheme(enabled: Boolean) = sessionStore.setDarkTheme(enabled)

    // ── Requester Flows & Sync ───────────────────────────────────────────────

    fun observeRequesterSummary(): Flow<Pair<RequesterSummaryDto, Long?>> =
        dao.observeSyncMeta(META_REQUESTER_SUMMARY).map { meta ->
            val dto = meta?.extraPayloadJson?.takeIf { it.isNotBlank() }?.let {
                runCatching { requesterSummaryAdapter.fromJson(it) }.getOrNull()
            } ?: RequesterSummaryDto()
            dto to meta?.lastSyncedMillis
        }

    fun observeRequests(): Flow<List<PurchaseRequestDto>> =
        dao.observeRequests().map { list ->
            list.map { e ->
                PurchaseRequestDto(
                    id = e.id,
                    requestNumber = e.requestNumber,
                    status = e.status,
                    statusLabel = e.statusLabel,
                    createdAt = e.createdAt,
                    createdAtJalali = e.createdAtJalali,
                    createdDateIso = e.createdDateIso,
                    deliveryDate = e.deliveryDate,
                    deliveryDateJalali = e.deliveryDateJalali,
                    deliveryImmediate = e.deliveryImmediate,
                    rejectionReason = e.rejectionReason,
                    canEdit = e.canEdit,
                    items = runCatching { requestItemsAdapter.fromJson(e.itemsJson) }.getOrNull().orEmpty(),
                )
            }
        }

    fun observeReceivedItems(): Flow<List<ReceivedItemDto>> =
        dao.observeReceivedItems().map { list ->
            list.map { e ->
                ReceivedItemDto(
                    itemId = e.itemId,
                    requestNumber = e.requestNumber,
                    name = e.name,
                    requestedQty = e.requestedQty,
                    unit = e.unit,
                    deliveries = runCatching { deliveryPartsAdapter.fromJson(e.deliveriesJson) }.getOrNull().orEmpty(),
                    isConfirmed = e.isConfirmed,
                    confirmedAtJalali = e.confirmedAtJalali,
                )
            }
        }

    fun observeProducts(): Flow<List<ProductCatalogDto>> =
        dao.observeProducts().map { list ->
            list.map { e ->
                ProductCatalogDto(
                    id = e.id,
                    name = e.name,
                    unitName = e.unitName,
                    categoryName = e.categoryName,
                    currentStock = e.currentStock,
                    isBlocked = e.isBlocked,
                    blockedReason = e.blockedReason,
                )
            }
        }

    fun observeUnits(): Flow<List<String>> =
        dao.observeSyncMeta(META_UNITS).map { meta ->
            meta?.extraPayloadJson?.takeIf { it.isNotBlank() }?.let {
                runCatching { stringListAdapter.fromJson(it) }.getOrNull()
            }?.takeIf { it.isNotEmpty() } ?: DEFAULT_UNITS
        }

    fun observeSyncTime(resourceKey: String): Flow<Long?> =
        dao.observeSyncMeta(resourceKey).map { it?.lastSyncedMillis }

    suspend fun syncRequesterAll(): Result<Unit> = runCatching {
        val cafe = activeCafe()
        if (cafe.isBlank()) return@runCatching
        val now = System.currentTimeMillis()

        val summary = api.getRequesterSummary(cafe)
        dao.upsertSyncMeta(
            SyncMetaEntity(META_REQUESTER_SUMMARY, now, requesterSummaryAdapter.toJson(summary))
        )

        val requestsRes = api.getRequesterRequests(cafe)
        dao.replaceRequests(
            requestsRes.requests.mapIndexed { idx, r ->
                CachedRequestEntity(
                    id = r.id,
                    requestNumber = r.requestNumber,
                    status = r.status,
                    statusLabel = r.statusLabel,
                    createdAt = r.createdAt,
                    createdAtJalali = r.createdAtJalali,
                    createdDateIso = r.createdDateIso,
                    deliveryDate = r.deliveryDate,
                    deliveryDateJalali = r.deliveryDateJalali,
                    deliveryImmediate = r.deliveryImmediate,
                    rejectionReason = r.rejectionReason,
                    canEdit = r.canEdit,
                    itemsJson = requestItemsAdapter.toJson(r.items),
                    sortOrder = idx,
                )
            }
        )
        dao.upsertSyncMeta(SyncMetaEntity(META_REQUESTS, now))

        val receivedRes = api.getReceivedItems(cafe)
        val combined = receivedRes.pending + receivedRes.confirmed
        dao.replaceReceivedItems(
            combined.mapIndexed { idx, item ->
                CachedReceivedItemEntity(
                    itemId = item.itemId,
                    requestNumber = item.requestNumber,
                    name = item.name,
                    requestedQty = item.requestedQty,
                    unit = item.unit,
                    deliveriesJson = deliveryPartsAdapter.toJson(item.deliveries),
                    isConfirmed = item.isConfirmed,
                    confirmedAtJalali = item.confirmedAtJalali,
                    sortOrder = idx,
                )
            }
        )
        dao.upsertSyncMeta(SyncMetaEntity(META_RECEIVED, now))

        val catalogRes = api.getRequesterProducts(cafe)
        dao.replaceProducts(
            catalogRes.products.map { p ->
                CachedProductEntity(
                    id = p.id,
                    name = p.name,
                    unitName = p.unitName,
                    categoryName = p.categoryName,
                    currentStock = p.currentStock,
                    isBlocked = p.isBlocked,
                    blockedReason = p.blockedReason,
                )
            }
        )
        dao.upsertSyncMeta(SyncMetaEntity(META_UNITS, now, stringListAdapter.toJson(catalogRes.units)))
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun createRequest(payload: CreateRequestPayload): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.createRequesterRequest(cafe, payload)
        syncRequesterAll()
        res.detail.ifBlank { "درخواست شما با موفقیت ثبت شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun editRequest(requestId: Int, payload: CreateRequestPayload): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.editRequesterRequest(cafe, requestId, payload)
        syncRequesterAll()
        res.detail.ifBlank { "درخواست به‌روزرسانی شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun confirmReceived(itemId: Int? = null, confirmAll: Boolean = false): Result<String> =
        runCatching {
            val cafe = activeCafe()
            val payload = if (confirmAll) {
                ConfirmReceivedPayload(action = "confirm_all")
            } else {
                ConfirmReceivedPayload(action = "confirm_one", itemId = itemId)
            }
            val res = api.confirmReceivedItems(cafe, payload)
            syncRequesterAll()
            res.detail.ifBlank { "تایید دریافت ثبت شد." }
        }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    // ── Warehouse Flows & Sync ───────────────────────────────────────────────

    fun observeWarehouseMeta(): Flow<Pair<WarehouseDashboardMeta, Long?>> =
        dao.observeSyncMeta(META_WAREHOUSE_DASHBOARD).map { meta ->
            val parsed = meta?.extraPayloadJson?.takeIf { it.isNotBlank() }?.let {
                runCatching { warehouseMetaAdapter.fromJson(it) }.getOrNull()
            } ?: WarehouseDashboardMeta()
            parsed to meta?.lastSyncedMillis
        }

    fun observeWarehouseCards(): Flow<List<WarehouseRequestCardDto>> =
        dao.observeWarehouseCards().map { list ->
            list.map { e ->
                WarehouseRequestCardDto(
                    requestId = e.requestId,
                    requestNumber = e.requestNumber,
                    requesterName = e.requesterName,
                    departmentName = e.departmentName,
                    createdAtJalali = e.createdAtJalali,
                    deliveryDateJalali = e.deliveryDateJalali,
                    deliveryImmediate = e.deliveryImmediate,
                    visibleItems = runCatching { visibleItemsAdapter.fromJson(e.visibleItemsJson) }.getOrNull().orEmpty(),
                    doneItems = runCatching { doneItemsAdapter.fromJson(e.doneItemsJson) }.getOrNull().orEmpty(),
                )
            }
        }

    fun observeDraftItems(): Flow<List<DraftItemDto>> =
        dao.observeDraftItems().map { list ->
            list.map { e ->
                DraftItemDto(
                    id = e.id,
                    productId = e.productId,
                    name = e.name,
                    quantity = e.quantity,
                    unit = e.unit,
                    description = e.description,
                    needsCeo = e.needsCeo,
                    ceoReasons = runCatching { stringListAdapter.fromJson(e.ceoReasonsJson) }.getOrNull().orEmpty(),
                    fromRequest = e.fromRequest,
                )
            }
        }

    fun observeDraftOfficers(): Flow<List<OfficerOptionDto>> =
        dao.observeSyncMeta(META_WAREHOUSE_DRAFT).map { meta ->
            meta?.extraPayloadJson?.takeIf { it.isNotBlank() }?.let {
                runCatching { officerListAdapter.fromJson(it) }.getOrNull()
            }.orEmpty()
        }

    fun observeStockItems(): Flow<List<StockItemDto>> =
        dao.observeStockItems().map { list ->
            list.map { e ->
                StockItemDto(
                    id = e.id,
                    productId = e.productId,
                    productName = e.productName,
                    categoryName = e.categoryName,
                    warehouseId = e.warehouseId,
                    warehouseName = e.warehouseName,
                    quantity = e.quantity,
                    minQuantity = e.minQuantity,
                    unitName = e.unitName,
                    isLowStock = e.isLowStock,
                    lastUpdatedJalali = e.lastUpdatedJalali,
                )
            }
        }

    fun observeWarehousesList(): Flow<List<WarehouseOptionDto>> =
        dao.observeSyncMeta(META_WAREHOUSE_STOCK).map { meta ->
            meta?.extraPayloadJson?.takeIf { it.isNotBlank() }?.let {
                runCatching { warehouseListAdapter.fromJson(it) }.getOrNull()
            }.orEmpty()
        }

    fun observeVouchers(): Flow<List<VoucherDto>> =
        dao.observeVouchers().map { list ->
            list.map { e ->
                VoucherDto(
                    id = e.id,
                    voucherNumber = e.voucherNumber,
                    voucherType = e.voucherType,
                    voucherTypeLabel = e.voucherTypeLabel,
                    status = e.status,
                    statusLabel = e.statusLabel,
                    sourceWarehouse = e.sourceWarehouse,
                    destinationWarehouse = e.destinationWarehouse,
                    description = e.description,
                    dateJalali = e.dateJalali,
                    itemCount = e.itemCount,
                    itemsSummary = e.itemsSummary,
                )
            }
        }

    suspend fun syncWarehouseDashboard(day: String? = null, department: String? = null): Result<Unit> =
        runCatching {
            val cafe = activeCafe()
            if (cafe.isBlank()) return@runCatching
            val now = System.currentTimeMillis()
            val res = api.getWarehouseDashboard(cafe, day = day, department = department?.ifBlank { null })
            dao.replaceWarehouseCards(
                res.cards.mapIndexed { idx, c ->
                    CachedWarehouseCardEntity(
                        requestId = c.requestId,
                        requestNumber = c.requestNumber,
                        requesterName = c.requesterName,
                        departmentName = c.departmentName,
                        createdAtJalali = c.createdAtJalali,
                        deliveryDateJalali = c.deliveryDateJalali,
                        deliveryImmediate = c.deliveryImmediate,
                        visibleItemsJson = visibleItemsAdapter.toJson(c.visibleItems),
                        doneItemsJson = doneItemsAdapter.toJson(c.doneItems),
                        selectedDateIso = res.selectedDate,
                        sortOrder = idx,
                    )
                }
            )
            val meta = WarehouseDashboardMeta(
                selectedDate = res.selectedDate,
                selectedDateJalali = res.selectedDateJalali,
                todayIso = res.todayIso,
                stats = res.stats,
                departments = res.departments,
                draftCount = res.draftCount,
                myPurchasesCount = res.myPurchasesCount,
            )
            dao.upsertSyncMeta(SyncMetaEntity(META_WAREHOUSE_DASHBOARD, now, warehouseMetaAdapter.toJson(meta)))
        }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun syncWarehouseDraft(): Result<Unit> = runCatching {
        val cafe = activeCafe()
        if (cafe.isBlank()) return@runCatching
        val now = System.currentTimeMillis()
        val res = api.getWarehouseDraft(cafe)
        dao.replaceDraftItems(
            res.items.map { d ->
                CachedDraftItemEntity(
                    id = d.id,
                    productId = d.productId,
                    name = d.name,
                    quantity = d.quantity,
                    unit = d.unit,
                    description = d.description,
                    needsCeo = d.needsCeo,
                    ceoReasonsJson = stringListAdapter.toJson(d.ceoReasons),
                    fromRequest = d.fromRequest,
                )
            }
        )
        dao.upsertSyncMeta(SyncMetaEntity(META_WAREHOUSE_DRAFT, now, officerListAdapter.toJson(res.officers)))
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun syncWarehouseStockAndVouchers(): Result<Unit> = runCatching {
        val cafe = activeCafe()
        if (cafe.isBlank()) return@runCatching
        val now = System.currentTimeMillis()
        val stockRes = api.getWarehouseStock(cafe)
        dao.replaceStockItems(
            stockRes.items.map { s ->
                CachedStockItemEntity(
                    id = s.id,
                    productId = s.productId,
                    productName = s.productName,
                    categoryName = s.categoryName,
                    warehouseId = s.warehouseId,
                    warehouseName = s.warehouseName,
                    quantity = s.quantity,
                    minQuantity = s.minQuantity,
                    unitName = s.unitName,
                    isLowStock = s.isLowStock,
                    lastUpdatedJalali = s.lastUpdatedJalali,
                )
            }
        )
        dao.upsertSyncMeta(
            SyncMetaEntity(META_WAREHOUSE_STOCK, now, warehouseListAdapter.toJson(stockRes.warehouses))
        )

        val vouchersRes = api.getWarehouseVouchers(cafe)
        dao.replaceVouchers(
            vouchersRes.vouchers.map { v ->
                CachedVoucherEntity(
                    id = v.id,
                    voucherNumber = v.voucherNumber,
                    voucherType = v.voucherType,
                    voucherTypeLabel = v.voucherTypeLabel,
                    status = v.status,
                    statusLabel = v.statusLabel,
                    sourceWarehouse = v.sourceWarehouse,
                    destinationWarehouse = v.destinationWarehouse,
                    description = v.description,
                    dateJalali = v.dateJalali,
                    itemCount = v.itemCount,
                    itemsSummary = v.itemsSummary,
                )
            }
        )
        dao.upsertSyncMeta(SyncMetaEntity(META_WAREHOUSE_VOUCHERS, now))

        // Refresh catalog for manual draft & voucher item selection
        val catalogRes = api.getRequesterProducts(cafe)
        dao.replaceProducts(
            catalogRes.products.map { p ->
                CachedProductEntity(
                    id = p.id,
                    name = p.name,
                    unitName = p.unitName,
                    categoryName = p.categoryName,
                    currentStock = p.currentStock,
                    isBlocked = p.isBlocked,
                    blockedReason = p.blockedReason,
                )
            }
        )
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun performWarehouseAction(
        itemId: Int,
        action: String,
        allocations: List<StockAllocationPayload> = emptyList(),
        reason: String = "",
        day: String? = null,
        department: String? = null,
    ): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.performWarehouseItemAction(
            cafe,
            itemId,
            WarehouseItemActionPayload(action = action, allocations = allocations, reason = reason),
        )
        syncWarehouseDashboard(day, department)
        if (action == "to_purchase" || action == "reopen") {
            syncWarehouseDraft()
        }
        res.detail.ifBlank { "عملیات ثبت شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun addDraftItem(payload: AddDraftItemPayload): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.addWarehouseDraftItem(cafe, payload)
        syncWarehouseDraft()
        res.detail.ifBlank { "کالا به پیش‌نویس اضافه شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun updateDraftItem(itemId: Int, quantity: String, description: String): Result<String> =
        runCatching {
            val cafe = activeCafe()
            val res = api.updateWarehouseDraftItem(cafe, itemId, UpdateDraftItemPayload(quantity, description))
            syncWarehouseDraft()
            res.detail.ifBlank { "قلم به‌روزرسانی شد." }
        }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun deleteDraftItem(itemId: Int): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.deleteWarehouseDraftItem(cafe, itemId)
        syncWarehouseDraft()
        res.detail.ifBlank { "قلم حذف شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun sendDraft(selectedIds: List<Int>, officerUserId: String): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.sendWarehouseDraft(cafe, SendDraftPayload(selectedIds, officerUserId))
        syncWarehouseDraft()
        syncWarehouseDashboard()
        if (officerUserId == "self") {
            syncPurchasing(scope = SCOPE_WAREHOUSE_SELF)
        }
        res.detail.ifBlank { "پیش‌نویس خرید ارسال شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun getStockTransactions(stockId: Int): Result<StockTransactionsResponseDto> = runCatching {
        api.getStockTransactions(activeCafe(), stockId)
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun createVoucher(payload: CreateVoucherPayload): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.createWarehouseVoucher(cafe, payload)
        syncWarehouseStockAndVouchers()
        res.detail.ifBlank { "حواله انبار با موفقیت ثبت شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    // ── Purchasing Flows & Sync ──────────────────────────────────────────────

    fun observePurchasingMeta(scope: String): Flow<Pair<PurchasingMeta, Long?>> =
        dao.observeSyncMeta("purchasing_meta_$scope").map { meta ->
            val parsed = meta?.extraPayloadJson?.takeIf { it.isNotBlank() }?.let {
                runCatching { purchasingMetaAdapter.fromJson(it) }.getOrNull()
            } ?: PurchasingMeta()
            parsed to meta?.lastSyncedMillis
        }

    fun observePurchaseRows(scope: String): Flow<List<PurchaseRowDto>> =
        dao.observePurchaseRows(scope).map { list ->
            list.map { e ->
                PurchaseRowDto(
                    rowKey = e.rowKey,
                    name = e.name,
                    unit = e.unit,
                    totalQty = e.totalQty,
                    sections = runCatching { sectionListAdapter.fromJson(e.sectionsJson) }.getOrNull().orEmpty(),
                    assignmentIds = runCatching { intListAdapter.fromJson(e.assignmentIdsJson) }.getOrNull().orEmpty(),
                    descriptions = runCatching { stringListAdapter.fromJson(e.descriptionsJson) }.getOrNull().orEmpty(),
                )
            }
        }

    fun observePurchaseHistory(scope: String): Flow<List<PurchaseHistoryItemDto>> =
        dao.observePurchaseHistory(scope).map { list ->
            list.map { e ->
                PurchaseHistoryItemDto(
                    assignmentId = e.assignmentId,
                    name = e.name,
                    quantity = e.quantity,
                    unit = e.unit,
                    departmentName = e.departmentName,
                    requestNumber = e.requestNumber,
                    status = e.status,
                    statusLabel = e.statusLabel,
                    dateJalali = e.dateJalali,
                    timeStr = e.timeStr,
                    notes = e.notes,
                )
            }
        }

    suspend fun syncPurchasing(scope: String = SCOPE_PURCHASING): Result<Unit> = runCatching {
        val cafe = activeCafe()
        if (cafe.isBlank()) return@runCatching
        val now = System.currentTimeMillis()

        val dash = api.getPurchasingDashboard(cafe, scope = scope)
        dao.replacePurchaseRows(
            scope,
            dash.rows.mapIndexed { idx, r ->
                CachedPurchaseRowEntity(
                    cacheKey = "${scope}_${r.rowKey}_$idx",
                    scope = scope,
                    rowKey = r.rowKey,
                    name = r.name,
                    unit = r.unit,
                    totalQty = r.totalQty,
                    sectionsJson = sectionListAdapter.toJson(r.sections),
                    assignmentIdsJson = intListAdapter.toJson(r.assignmentIds),
                    descriptionsJson = stringListAdapter.toJson(r.descriptions),
                    sortOrder = idx,
                )
            }
        )
        dao.upsertSyncMeta(
            SyncMetaEntity(
                "purchasing_meta_$scope",
                now,
                purchasingMetaAdapter.toJson(PurchasingMeta(dash.totalItems, dash.awaitingCeoCount)),
            )
        )

        val hist = api.getPurchasingHistory(cafe, scope = scope)
        dao.replacePurchaseHistory(
            scope,
            hist.items.mapIndexed { idx, h ->
                CachedPurchaseHistoryEntity(
                    cacheKey = "${scope}_${h.assignmentId}",
                    scope = scope,
                    assignmentId = h.assignmentId,
                    name = h.name,
                    quantity = h.quantity,
                    unit = h.unit,
                    departmentName = h.departmentName,
                    requestNumber = h.requestNumber,
                    status = h.status,
                    statusLabel = h.statusLabel,
                    dateJalali = h.dateJalali,
                    timeStr = h.timeStr,
                    notes = h.notes,
                    sortOrder = idx,
                )
            }
        )
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    suspend fun performPurchasingAction(
        assignmentIds: List<Int>,
        action: String,
        scope: String = SCOPE_PURCHASING,
        notes: String = "",
    ): Result<String> = runCatching {
        val cafe = activeCafe()
        val res = api.performPurchasingAction(
            cafe,
            PurchasingActionPayload(action = action, assignmentIds = assignmentIds, scope = scope, notes = notes),
        )
        syncPurchasing(scope)
        res.detail.ifBlank { "وضعیت خرید ثبت شد." }
    }.recoverCatching { throw IllegalStateException(NetworkModule.parseErrorMessage(it)) }

    companion object {
        const val META_REQUESTER_SUMMARY = "requester_summary"
        const val META_REQUESTS = "requester_requests"
        const val META_RECEIVED = "requester_received"
        const val META_UNITS = "requester_units"
        const val META_WAREHOUSE_DASHBOARD = "warehouse_dashboard"
        const val META_WAREHOUSE_DRAFT = "warehouse_draft"
        const val META_WAREHOUSE_STOCK = "warehouse_stock"
        const val META_WAREHOUSE_VOUCHERS = "warehouse_vouchers"
        const val SCOPE_PURCHASING = "purchasing"
        const val SCOPE_WAREHOUSE_SELF = "warehouse_self"

        val DEFAULT_UNITS = listOf("عدد", "کیلوگرم", "گرم", "لیتر", "بسته", "کارتن")
    }
}
