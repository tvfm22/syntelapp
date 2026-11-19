package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.AddDraftItemPayload
import com.example.data.remote.CreateVoucherPayload
import com.example.data.remote.DraftItemDto
import com.example.data.remote.OfficerOptionDto
import com.example.data.remote.ProductCatalogDto
import com.example.data.remote.StockAllocationPayload
import com.example.data.remote.StockItemDto
import com.example.data.remote.StockTransactionsResponseDto
import com.example.data.remote.VoucherDto
import com.example.data.remote.WarehouseOptionDto
import com.example.data.remote.WarehouseRequestCardDto
import com.example.data.repository.SyntelRepository
import com.example.data.repository.WarehouseDashboardMeta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class WarehouseUiState(
    val meta: WarehouseDashboardMeta = WarehouseDashboardMeta(),
    val cards: List<WarehouseRequestCardDto> = emptyList(),
    val draftItems: List<DraftItemDto> = emptyList(),
    val officers: List<OfficerOptionDto> = emptyList(),
    val stockItems: List<StockItemDto> = emptyList(),
    val warehouses: List<WarehouseOptionDto> = emptyList(),
    val vouchers: List<VoucherDto> = emptyList(),
    val products: List<ProductCatalogDto> = emptyList(),
    val lastSyncedMillis: Long? = null,
    val selectedDayIso: String = "",
    val selectedDepartment: String = "",
    val stockWarehouseFilterId: Int? = null,
    val lowStockOnly: Boolean = false,
    val stockSearchQuery: String = "",
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
)

class WarehouseViewModel(
    private val repository: SyntelRepository,
) : ViewModel() {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _selectedDayIso = MutableStateFlow(isoFormat.format(Calendar.getInstance().time))
    private val _selectedDepartment = MutableStateFlow("")
    private val _stockWarehouseFilterId = MutableStateFlow<Int?>(null)
    private val _lowStockOnly = MutableStateFlow(false)
    private val _stockSearchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _isSubmitting = MutableStateFlow(false)

    private val _selectedStockTx = MutableStateFlow<StockTransactionsResponseDto?>(null)
    val selectedStockTx: StateFlow<StockTransactionsResponseDto?> = _selectedStockTx.asStateFlow()

    private val _loadingTx = MutableStateFlow(false)
    val loadingTx: StateFlow<Boolean> = _loadingTx.asStateFlow()

    private val coreFlow = combine(
        repository.observeWarehouseMeta(),
        repository.observeWarehouseCards(),
        repository.observeDraftItems(),
        repository.observeDraftOfficers(),
        repository.observeStockItems(),
    ) { metaPair, cards, draft, officers, stock ->
        WarehouseUiState(
            meta = metaPair.first,
            lastSyncedMillis = metaPair.second,
            cards = cards,
            draftItems = draft,
            officers = officers,
            stockItems = stock,
        )
    }

    private val secondaryFlow = combine(
        repository.observeWarehousesList(),
        repository.observeVouchers(),
        repository.observeProducts(),
        _isRefreshing,
        _isSubmitting,
    ) { warehouses, vouchers, products, refreshing, submitting ->
        SecondaryWarehouseBundle(warehouses, vouchers, products, refreshing, submitting)
    }

    private val filterFlow = combine(
        _selectedDayIso,
        _selectedDepartment,
        _stockWarehouseFilterId,
        _lowStockOnly,
        _stockSearchQuery,
    ) { day, dept, whId, lowStock, search ->
        WarehouseFilterBundle(day, dept, whId, lowStock, search)
    }

    val uiState: StateFlow<WarehouseUiState> = combine(
        coreFlow,
        secondaryFlow,
        filterFlow,
    ) { core, sec, filter ->
        core.copy(
            warehouses = sec.warehouses,
            vouchers = sec.vouchers,
            products = sec.products,
            isRefreshing = sec.isRefreshing,
            isSubmitting = sec.isSubmitting,
            selectedDayIso = filter.day,
            selectedDepartment = filter.department,
            stockWarehouseFilterId = filter.warehouseId,
            lowStockOnly = filter.lowStockOnly,
            stockSearchQuery = filter.stockSearch,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WarehouseUiState(),
    )

    fun refreshAll(onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val r1 = repository.syncWarehouseDashboard(_selectedDayIso.value, _selectedDepartment.value)
            val r2 = repository.syncWarehouseDraft()
            val r3 = repository.syncWarehouseStockAndVouchers()
            val firstErr = r1.exceptionOrNull() ?: r2.exceptionOrNull() ?: r3.exceptionOrNull()
            if (firstErr != null) {
                onError(firstErr.message ?: "خطا در همگام‌سازی اطلاعات انبار.")
            }
            _isRefreshing.value = false
        }
    }

    fun shiftDay(deltaDays: Int, onError: (String) -> Unit = {}) {
        val cal = Calendar.getInstance()
        try {
            val parsed = isoFormat.parse(_selectedDayIso.value)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {
        }
        cal.add(Calendar.DAY_OF_MONTH, deltaDays)
        _selectedDayIso.value = isoFormat.format(cal.time)
        refreshQueueOnly(onError)
    }

    fun jumpToToday(onError: (String) -> Unit = {}) {
        _selectedDayIso.value = isoFormat.format(Calendar.getInstance().time)
        refreshQueueOnly(onError)
    }

    fun setDepartmentFilter(dept: String, onError: (String) -> Unit = {}) {
        _selectedDepartment.value = dept
        refreshQueueOnly(onError)
    }

    private fun refreshQueueOnly(onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncWarehouseDashboard(_selectedDayIso.value, _selectedDepartment.value)
                .onFailure { onError(it.message ?: "خطا در دریافت صف انبار.") }
            _isRefreshing.value = false
        }
    }

    fun setStockWarehouseFilter(warehouseId: Int?) {
        _stockWarehouseFilterId.value = warehouseId
    }

    fun toggleLowStockOnly() {
        _lowStockOnly.value = !_lowStockOnly.value
    }

    fun setStockSearchQuery(query: String) {
        _stockSearchQuery.value = query
    }

    fun performItemAction(
        itemId: Int,
        action: String,
        allocations: List<StockAllocationPayload> = emptyList(),
        reason: String = "",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.performWarehouseAction(
                itemId = itemId,
                action = action,
                allocations = allocations,
                reason = reason,
                day = _selectedDayIso.value,
                department = _selectedDepartment.value,
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = onSuccess,
                onFailure = { onError(it.message ?: "خطا در ثبت عملیات انبار.") },
            )
        }
    }

    fun addDraftItem(
        productId: Int?,
        name: String,
        quantity: String,
        unit: String,
        description: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.addDraftItem(
                AddDraftItemPayload(productId, name.trim(), quantity.trim(), unit.trim(), description.trim())
            )
            _isSubmitting.value = false
            res.fold(onSuccess = onSuccess, onFailure = { onError(it.message ?: "خطا در افزودن به پیش‌نویس.") })
        }
    }

    fun updateDraftItem(
        itemId: Int,
        quantity: String,
        description: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.updateDraftItem(itemId, quantity.trim(), description.trim())
            _isSubmitting.value = false
            res.fold(onSuccess = onSuccess, onFailure = { onError(it.message ?: "خطا در ویرایش پیش‌نویس.") })
        }
    }

    fun deleteDraftItem(
        itemId: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.deleteDraftItem(itemId)
            _isSubmitting.value = false
            res.fold(onSuccess = onSuccess, onFailure = { onError(it.message ?: "خطا در حذف قلم.") })
        }
    }

    fun sendDraft(
        selectedIds: List<Int>,
        officerUserId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.sendDraft(selectedIds, officerUserId)
            _isSubmitting.value = false
            res.fold(onSuccess = onSuccess, onFailure = { onError(it.message ?: "خطا در ارسال پیش‌نویس خرید.") })
        }
    }

    fun loadStockTransactions(stockId: Int, onError: (String) -> Unit) {
        viewModelScope.launch {
            _loadingTx.value = true
            val res = repository.getStockTransactions(stockId)
            _loadingTx.value = false
            res.fold(
                onSuccess = { _selectedStockTx.value = it },
                onFailure = { onError(it.message ?: "خطا در دریافت گردش کالا.") },
            )
        }
    }

    fun clearStockTransactions() {
        _selectedStockTx.value = null
    }

    fun createVoucher(
        payload: CreateVoucherPayload,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.createVoucher(payload)
            _isSubmitting.value = false
            res.fold(onSuccess = onSuccess, onFailure = { onError(it.message ?: "خطا در ثبت حواله انبار.") })
        }
    }

    private data class SecondaryWarehouseBundle(
        val warehouses: List<WarehouseOptionDto>,
        val vouchers: List<VoucherDto>,
        val products: List<ProductCatalogDto>,
        val isRefreshing: Boolean,
        val isSubmitting: Boolean,
    )

    private data class WarehouseFilterBundle(
        val day: String,
        val department: String,
        val warehouseId: Int?,
        val lowStockOnly: Boolean,
        val stockSearch: String,
    )

    companion object {
        fun factory(repository: SyntelRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WarehouseViewModel(repository) as T
                }
            }
    }
}
