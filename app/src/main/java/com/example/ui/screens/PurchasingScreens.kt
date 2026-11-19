package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserSession
import com.example.data.remote.PurchaseHistoryItemDto
import com.example.data.remote.PurchaseRowDto
import com.example.data.repository.SyntelRepository
import com.example.ui.components.BadgeTone
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.SyntelHeaderCard
import com.example.ui.components.SyntelSearchBar
import com.example.ui.components.requestStatusTone
import com.example.ui.theme.LocalSyntelColors
import com.example.viewmodel.PurchasingUiState

private data class PendingPurchaseAction(
    val row: PurchaseRowDto,
    val action: String,
    val title: String,
    val confirmLabel: String,
    val notesHint: String,
)

@Composable
fun PurchasingDashboardScreen(
    session: UserSession,
    state: PurchasingUiState,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onPerformAction: (assignmentIds: List<Int>, action: String, notes: String) -> Unit,
    onSwitchToHistory: (() -> Unit)? = null,
) {
    val isSelfBuy = state.scope == SyntelRepository.SCOPE_WAREHOUSE_SELF
    var pendingAction by remember { mutableStateOf<PendingPurchaseAction?>(null) }
    var actionNote by remember { mutableStateOf("") }

    val filteredRows = remember(state.rows, state.searchQuery) {
        val q = state.searchQuery.trim()
        if (q.isEmpty()) state.rows
        else state.rows.filter { row ->
            row.name.contains(q, ignoreCase = true) ||
                row.sections.any { it.name.contains(q, ignoreCase = true) } ||
                row.descriptions.any { it.contains(q, ignoreCase = true) }
        }
    }

    val cs = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SyntelHeaderCard(
                title = if (isSelfBuy) "خریدهای من (انبار)" else "لیست خرید من",
                subtitle = "${session.fullName.ifBlank { session.username }} • ${session.cafeName}",
                badgeText = if (isSelfBuy) "خرید مستقیم انبار" else "واحد خرید",
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeaderPill(label = "اقلام فعال: ${state.meta.totalItems}")
                    if (state.meta.awaitingCeoCount > 0) {
                        HeaderPill(label = "در انتظار مدیرعامل: ${state.meta.awaitingCeoCount}")
                    }
                    if (onSwitchToHistory != null) {
                        Spacer(Modifier.weight(1f))
                        Surface(
                            onClick = onSwitchToHistory,
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("self_buy_history_button"),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "سوابق",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OfflineStatusBanner(
                isOnline = isOnline,
                lastSyncedMillis = state.lastSyncedMillis,
                onRetry = onRefresh,
            )
        }

        if (state.meta.awaitingCeoCount > 0) {
            item {
                val sem = LocalSyntelColors.current
                Surface(
                    color = sem.warningContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, sem.warning.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.HourglassTop,
                            contentDescription = null,
                            tint = sem.warning,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${state.meta.awaitingCeoCount} قلم در انتظار تایید مدیرعامل است و پس از تایید به این لیست اضافه می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurface,
                        )
                    }
                }
            }
        }

        item {
            SyntelSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "جستجوی کالا یا بخش درخواست‌کننده...",
                testTag = "purchasing_search_input",
            )
        }

        if (filteredRows.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.ShoppingBag,
                    title = "قلم خرید فعالی وجود ندارد",
                    description = "پس از ارسال پیش‌نویس خرید از انبار، اقلام تجمیع‌شده اینجا نمایش داده می‌شوند.",
                )
            }
        } else {
            items(filteredRows, key = { it.rowKey }) { row ->
                PurchaseRowCard(
                    row = row,
                    isOnline = isOnline,
                    isSubmitting = state.isSubmitting,
                    onMarkPurchased = {
                        onPerformAction(row.assignmentIds, "purchased", "")
                    },
                    onMarkNotPurchased = {
                        actionNote = ""
                        pendingAction = PendingPurchaseAction(
                            row = row,
                            action = "not_purchased",
                            title = "ثبت به عنوان خریداری نشده",
                            confirmLabel = "ثبت خریداری نشد",
                            notesHint = "دلیل عدم خرید (مثلاً عدم موجودی در بازار یا افزایش قیمت)...",
                        )
                    },
                    onReturn = {
                        actionNote = ""
                        pendingAction = PendingPurchaseAction(
                            row = row,
                            action = "return",
                            title = "بازگشت کالا به انبار",
                            confirmLabel = "تایید بازگشت",
                            notesHint = "توضیحات بازگشت (اختیاری)...",
                        )
                    },
                )
            }
        }
    }

    pendingAction?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(pending.title, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${pending.row.name} — ${pending.row.totalQty} ${pending.row.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedTextField(
                        value = actionNote,
                        onValueChange = { actionNote = it },
                        placeholder = { Text(pending.notesHint) },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("purchasing_note_input"),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPerformAction(pending.row.assignmentIds, pending.action, actionNote)
                        pendingAction = null
                    },
                    modifier = Modifier.testTag("purchasing_confirm_dialog_button"),
                ) {
                    Text(pending.confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) {
                    Text("انصراف")
                }
            },
        )
    }
}

@Composable
private fun HeaderPill(label: String) {
    Surface(
        color = Color.White.copy(alpha = 0.2f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PurchaseRowCard(
    row: PurchaseRowDto,
    isOnline: Boolean,
    isSubmitting: Boolean,
    onMarkPurchased: () -> Unit,
    onMarkNotPurchased: () -> Unit,
    onReturn: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("purchase_row_card_${row.rowKey}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, sem.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(
                    label = "${row.totalQty} ${row.unit}",
                    tone = BadgeTone.PRIMARY,
                )
            }

            if (row.sections.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    row.sections.forEach { sec ->
                        Surface(
                            color = cs.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = "${sec.name}: ${sec.qty}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            if (row.descriptions.isNotEmpty()) {
                Surface(
                    color = sem.infoContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = row.descriptions.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onMarkPurchased,
                    enabled = isOnline && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sem.success,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("mark_purchased_${row.rowKey}"),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("خریداری شد", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onMarkNotPurchased,
                    enabled = isOnline && !isSubmitting,
                    border = BorderStroke(1.dp, sem.danger.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = sem.danger),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_not_purchased_${row.rowKey}"),
                ) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("نشد", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = onReturn,
                    enabled = isOnline && !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("mark_return_${row.rowKey}"),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("بازگشت", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun PurchasingHistoryScreen(
    state: PurchasingUiState,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onBackToList: (() -> Unit)? = null,
) {
    val filtered = remember(state.history, state.historyStatusFilter, state.searchQuery) {
        val q = state.searchQuery.trim()
        state.history.filter { item ->
            val matchStatus = when (state.historyStatusFilter) {
                "all" -> true
                else -> item.status == state.historyStatusFilter
            }
            val matchQuery = q.isEmpty() ||
                item.name.contains(q, ignoreCase = true) ||
                item.departmentName.contains(q, ignoreCase = true) ||
                item.requestNumber.contains(q, ignoreCase = true)
            matchStatus && matchQuery
        }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { it.dateJalali.ifBlank { "بدون تاریخ" } }
    }

    val cs = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.scope == SyntelRepository.SCOPE_WAREHOUSE_SELF) {
                            "سوابق خریدهای من (انبار)"
                        } else {
                            "سوابق خرید"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${filtered.size} مورد ثبت‌شده",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                if (onBackToList != null) {
                    TextButton(
                        onClick = onBackToList,
                        modifier = Modifier.testTag("back_to_self_buy_list"),
                    ) {
                        Text("بازگشت به لیست")
                    }
                }
            }
        }

        item {
            OfflineStatusBanner(
                isOnline = isOnline,
                lastSyncedMillis = state.lastSyncedMillis,
                onRetry = onRefresh,
            )
        }

        item {
            SyntelSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "جستجوی کالا، بخش یا شماره درخواست...",
                testTag = "purchasing_history_search",
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "all" to "همه",
                    "purchased" to "خریداری شده",
                    "not_purchased" to "خریداری نشده",
                    "returned" to "بازگشت داده شده",
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = state.historyStatusFilter == key,
                        onClick = { onStatusFilterChange(key) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("history_filter_$key"),
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.History,
                    title = "سابقه‌ای یافت نشد",
                    description = "اقلام خریداری‌شده یا ردشده با فیلتر انتخابی در اینجا نمایش داده می‌شوند.",
                )
            }
        } else {
            grouped.forEach { (dateLabel, dateItems) ->
                item(key = "header_$dateLabel") {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = cs.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                items(dateItems, key = { it.assignmentId }) { item ->
                    PurchaseHistoryCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun PurchaseHistoryCard(item: PurchaseHistoryItemDto) {
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("purchase_history_card_${item.assignmentId}"),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, sem.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(label = item.statusLabel, tone = requestStatusTone(item.status))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "مقدار: ${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.primary,
                )
                Text(
                    text = listOf(item.departmentName, item.requestNumber, item.timeStr)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
            if (item.notes.isNotBlank()) {
                Text(
                    text = "توضیحات: ${item.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}
