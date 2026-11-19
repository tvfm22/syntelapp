package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.CreateRequestItemPayload
import com.example.data.remote.CreateRequestPayload
import com.example.data.remote.ProductCatalogDto
import com.example.data.remote.PurchaseRequestDto
import com.example.data.remote.ReceivedItemDto
import com.example.data.remote.RequesterSummaryDto
import com.example.data.repository.SyntelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RequesterUiState(
    val summary: RequesterSummaryDto = RequesterSummaryDto(),
    val requests: List<PurchaseRequestDto> = emptyList(),
    val receivedItems: List<ReceivedItemDto> = emptyList(),
    val products: List<ProductCatalogDto> = emptyList(),
    val units: List<String> = SyntelRepository.DEFAULT_UNITS,
    val lastSyncedMillis: Long? = null,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val statusFilter: String = "all",
    val searchQuery: String = "",
)

class RequesterViewModel(
    private val repository: SyntelRepository,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _isSubmitting = MutableStateFlow(false)
    private val _statusFilter = MutableStateFlow("all")
    private val _searchQuery = MutableStateFlow("")

    private val baseDataFlow = combine(
        repository.observeRequesterSummary(),
        repository.observeRequests(),
        repository.observeReceivedItems(),
        repository.observeProducts(),
        repository.observeUnits(),
    ) { summaryPair, requests, received, products, units ->
        RequesterUiState(
            summary = summaryPair.first,
            lastSyncedMillis = summaryPair.second,
            requests = requests,
            receivedItems = received,
            products = products,
            units = units,
        )
    }

    val uiState: StateFlow<RequesterUiState> = combine(
        baseDataFlow,
        _isRefreshing,
        _isSubmitting,
        _statusFilter,
        _searchQuery,
    ) { base, refreshing, submitting, filter, query ->
        base.copy(
            isRefreshing = refreshing,
            isSubmitting = submitting,
            statusFilter = filter,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RequesterUiState(),
    )

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh(onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncRequesterAll().onFailure { err ->
                onError(err.message ?: "خطا در همگام‌سازی اطلاعات.")
            }
            _isRefreshing.value = false
        }
    }

    fun submitRequest(
        editingRequestId: Int?,
        deliveryDateIso: String,
        deliveryImmediate: Boolean,
        items: List<CreateRequestItemPayload>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val payload = CreateRequestPayload(
                deliveryDate = deliveryDateIso,
                deliveryImmediate = deliveryImmediate,
                items = items,
            )
            val result = if (editingRequestId == null) {
                repository.createRequest(payload)
            } else {
                repository.editRequest(editingRequestId, payload)
            }
            _isSubmitting.value = false
            result.fold(
                onSuccess = onSuccess,
                onFailure = { onError(it.message ?: "خطا در ثبت درخواست.") },
            )
        }
    }

    fun confirmReceived(
        itemId: Int? = null,
        confirmAll: Boolean = false,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = repository.confirmReceived(itemId = itemId, confirmAll = confirmAll)
            _isSubmitting.value = false
            result.fold(
                onSuccess = onSuccess,
                onFailure = { onError(it.message ?: "خطا در تایید دریافت.") },
            )
        }
    }

    companion object {
        fun factory(repository: SyntelRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RequesterViewModel(repository) as T
                }
            }
    }
}
