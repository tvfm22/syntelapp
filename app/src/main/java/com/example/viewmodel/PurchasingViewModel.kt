package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.PurchaseHistoryItemDto
import com.example.data.remote.PurchaseRowDto
import com.example.data.repository.PurchasingMeta
import com.example.data.repository.SyntelRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PurchasingUiState(
    val scope: String = SyntelRepository.SCOPE_PURCHASING,
    val meta: PurchasingMeta = PurchasingMeta(),
    val rows: List<PurchaseRowDto> = emptyList(),
    val history: List<PurchaseHistoryItemDto> = emptyList(),
    val lastSyncedMillis: Long? = null,
    val historyStatusFilter: String = "all",
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class PurchasingViewModel(
    private val repository: SyntelRepository,
) : ViewModel() {

    private val _scope = MutableStateFlow(SyntelRepository.SCOPE_PURCHASING)
    private val _historyStatusFilter = MutableStateFlow("all")
    private val _searchQuery = MutableStateFlow("")
    private val _isRefreshing = MutableStateFlow(false)
    private val _isSubmitting = MutableStateFlow(false)

    private val scopedDataFlow = _scope.flatMapLatest { scope ->
        combine(
            repository.observePurchasingMeta(scope),
            repository.observePurchaseRows(scope),
            repository.observePurchaseHistory(scope),
        ) { metaPair, rows, history ->
            PurchasingUiState(
                scope = scope,
                meta = metaPair.first,
                lastSyncedMillis = metaPair.second,
                rows = rows,
                history = history,
            )
        }
    }

    val uiState: StateFlow<PurchasingUiState> = combine(
        scopedDataFlow,
        _historyStatusFilter,
        _searchQuery,
        _isRefreshing,
        _isSubmitting,
    ) { base, statusFilter, query, refreshing, submitting ->
        base.copy(
            historyStatusFilter = statusFilter,
            searchQuery = query,
            isRefreshing = refreshing,
            isSubmitting = submitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PurchasingUiState(),
    )

    fun setScope(scope: String, onError: (String) -> Unit = {}) {
        if (_scope.value != scope) {
            _scope.value = scope
        }
        refresh(onError)
    }

    fun setHistoryStatusFilter(status: String) {
        _historyStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refresh(onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.syncPurchasing(_scope.value).onFailure { err ->
                onError(err.message ?: "خطا در همگام‌سازی لیست خرید.")
            }
            _isRefreshing.value = false
        }
    }

    fun performAction(
        assignmentIds: List<Int>,
        action: String,
        notes: String = "",
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.performPurchasingAction(
                assignmentIds = assignmentIds,
                action = action,
                scope = _scope.value,
                notes = notes.trim(),
            )
            _isSubmitting.value = false
            res.fold(
                onSuccess = onSuccess,
                onFailure = { onError(it.message ?: "خطا در ثبت وضعیت خرید.") },
            )
        }
    }

    companion object {
        fun factory(repository: SyntelRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PurchasingViewModel(repository) as T
                }
            }
    }
}
