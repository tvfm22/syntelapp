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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RemoveShoppingCart
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.UserSession
import com.example.data.remote.StockAllocationPayload
import com.example.data.remote.WarehouseDoneItemDto
import com.example.data.remote.WarehouseRequestCardDto
import com.example.data.remote.WarehouseVisibleItemDto
import com.example.ui.components.BadgeTone
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.SyntelHeaderCard
import com.example.ui.components.requestStatusTone
import com.example.ui.theme.LocalSyntelColors
import com.example.viewmodel.WarehouseUiState

@Composable
fun WarehouseQueueScreen(
    session: UserSession,
    state: WarehouseUiState,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onShiftDay: (Int) -> Unit,
    onJumpToday: () -> Unit,
    onSelectDepartment: (String) -> Unit,
    onFulfill: (itemId: Int, allocations: List<StockAllocationPayload>) -> Unit,
    onRouteToPurchase: (itemId: Int) -> Unit,
    onNotInStock: (itemId: Int) -> Unit,
    onReopen: (itemId: Int, reason: String) -> Unit,
) {
    var selectedVisibleItem by remember { mutableStateOf<WarehouseVisibleItemDto?>(null) }
    var reopenTargetItem by remember { mutableStateOf<WarehouseDoneItemDto?>(null) }

    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OfflineStatusBanner(
                isOnline = isOnline,
                lastSyncedMillis = state.lastSyncedMillis,
                onRetry = onRefresh,
            )
        }
        item {
            SyntelHeaderCard(
                title = "صف درخواست‌های انبار",
                subtitle = "${session.cafeName} • پیش‌نویس خرید: ${state.meta.draftCount} قلم",
                badgeText = "پنل مدیر / مسئول انبار",
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MiniStatPill("در انتظار", state.meta.stats.pending, Modifier.weight(1f))
                    MiniStatPill("کافی", state.meta.stats.enough, Modifier.weight(1f))
                    MiniStatPill("کسری", state.meta.stats.short, Modifier.weight(1f))
                    MiniStatPill("ثبت‌شده", state.meta.stats.done, Modifier.weight(1f))
                }
            }
        }

        // Date navigation bar
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                border = BorderStroke(1.dp, sem.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = { onShiftDay(-1) },
                        enabled = isOnline,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("warehouse_prev_day_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "روز قبل")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.meta.selectedDateJalali.ifBlank { state.selectedDayIso },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (state.meta.todayIso.isNotBlank() && state.selectedDayIso != state.meta.todayIso) {
                            TextButton(onClick = onJumpToday, enabled = isOnline) {
                                Text("بازگشت به امروز", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    IconButton(
                        onClick = { onShiftDay(1) },
                        enabled = isOnline,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("warehouse_next_day_button"),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "روز بعد")
                    }
                }
            }
        }

        // Department filter chips
        if (state.meta.departments.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.selectedDepartment.isBlank(),
                        onClick = { onSelectDepartment("") },
                        label = { Text("همه بخش‌ها") },
                    )
                    state.meta.departments.forEach { dept ->
                        FilterChip(
                            selected = state.selectedDepartment == dept,
                            onClick = { onSelectDepartment(dept) },
                            label = { Text(dept) },
                        )
                    }
                }
            }
        }

        if (state.cards.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Inventory2,
                    title = "درخواستی برای این روز ثبت نشده است",
                    description = "با فلش‌های بالا تاریخ را تغییر دهید یا دکمه همگام‌سازی را لمس کنید.",
                )
            }
        } else {
            items(state.cards, key = { it.requestId }) { card ->
                WarehouseRequestCard(
                    card = card,
                    isOnline = isOnline,
                    onOpenItemActions = { selectedVisibleItem = it },
                    onOpenReopen = { reopenTargetItem = it },
                )
            }
        }
    }

    selectedVisibleItem?.let { item ->
        WarehouseItemActionSheet(
            item = item,
            isOnline = isOnline,
            isSubmitting = state.isSubmitting,
            onDismiss = { selectedVisibleItem = null },
            onFulfill = { allocations ->
                onFulfill(item.itemId, allocations)
                selectedVisibleItem = null
            },
            onRouteToPurchase = {
                onRouteToPurchase(item.itemId)
                selectedVisibleItem = null
            },
            onNotInStock = {
                onNotInStock(item.itemId)
                selectedVisibleItem = null
            },
        )
    }

    reopenTargetItem?.let { doneItem ->
        ReopenReasonSheet(
            item = doneItem,
            isSubmitting = state.isSubmitting,
            onDismiss = { reopenTargetItem = null },
            onConfirm = { reason ->
                onReopen(doneItem.itemId, reason)
                reopenTargetItem = null
            },
        )
    }
}

@Composable
private fun MiniStatPill(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun WarehouseRequestCard(
    card: WarehouseRequestCardDto,
    isOnline: Boolean,
    onOpenItemActions: (WarehouseVisibleItemDto) -> Unit,
    onOpenReopen: (WarehouseDoneItemDto) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, sem.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${card.departmentName} • #${card.requestNumber}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "${card.requesterName} • ${card.createdAtJalali}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                if (card.deliveryImmediate) {
                    StatusBadge("فوری", BadgeTone.DANGER)
                } else if (card.deliveryDateJalali.isNotBlank()) {
                    StatusBadge("تحویل ${card.deliveryDateJalali}", BadgeTone.INFO)
                }
            }

            if (card.visibleItems.isNotEmpty()) {
                HorizontalDivider(color = sem.cardBorder)
                card.visibleItems.forEach { item ->
                    val stockTone = when {
                        item.isEnough -> BadgeTone.SUCCESS
                        item.hasStock -> BadgeTone.WARNING
                        else -> BadgeTone.DANGER
                    }
                    val stockText = when {
                        item.isEnough -> "موجودی کافی (${item.availableQty})"
                        item.hasStock -> "موجودی ناقص (${item.availableQty})"
                        else -> "ناموجود در انبار"
                    }
                    Surface(
                        color = cs.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "درخواستی: ${item.quantity} ${item.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                StatusBadge(stockText, stockTone)
                            }
                            if (item.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { onOpenItemActions(item) },
                                enabled = isOnline,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("warehouse_act_item_${item.itemId}"),
                            ) {
                                Text("تعیین وضعیت / تامین کالا")
                            }
                        }
                    }
                }
            }

            if (card.doneItems.isNotEmpty()) {
                HorizontalDivider(color = sem.cardBorder)
                Text(
                    text = "اقلام پردازش‌شده (${card.doneItems.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
                card.doneItems.forEach { done ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${done.name} (${done.quantity} ${done.unit})",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (done.suppliedQty.isNotBlank()) {
                                Text(
                                    text = "تامین‌شده: ${done.suppliedQty}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = sem.success,
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatusBadge(done.statusLabel, requestStatusTone(done.status))
                            if (done.isLocked) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = "قفل‌شده در انتظار تایید مدیرعامل",
                                    tint = cs.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            } else if (isOnline) {
                                IconButton(
                                    onClick = { onOpenReopen(done) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("warehouse_reopen_item_${done.itemId}"),
                                ) {
                                    Icon(
                                        Icons.Outlined.Refresh,
                                        contentDescription = "بازگشایی قلم",
                                        tint = cs.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarehouseItemActionSheet(
    item: WarehouseVisibleItemDto,
    isOnline: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onFulfill: (List<StockAllocationPayload>) -> Unit,
    onRouteToPurchase: () -> Unit,
    onNotInStock: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedStockId by remember(item.itemId) {
        mutableStateOf(item.stockOptions.firstOrNull()?.stockItemId)
    }
    var supplyQty by remember(item.itemId) { mutableStateOf(item.quantity) }
    val sem = LocalSyntelColors.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(item.name, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "مقدار درخواستی: ${item.quantity} ${item.unit} • کل موجودی: ${item.availableQty}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (item.stockOptions.isNotEmpty()) {
                Text("انتخاب انبار برای تامین:", style = MaterialTheme.typography.titleSmall)
                item.stockOptions.forEach { opt ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedStockId == opt.stockItemId,
                            onClick = { selectedStockId = opt.stockItemId },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(opt.warehouseName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "موجودی قابل برداشت: ${opt.availableQty} ${opt.unitLabel}",
                                style = MaterialTheme.typography.bodySmall,
                                color = sem.success,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = supplyQty,
                    onValueChange = { supplyQty = it },
                    label = { Text("مقدار تحویلی (${item.unit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("warehouse_fulfill_qty_input"),
                )

                Button(
                    onClick = {
                        val sid = selectedStockId ?: return@Button
                        onFulfill(listOf(StockAllocationPayload(sid, supplyQty.trim())))
                    },
                    enabled = isOnline && !isSubmitting && selectedStockId != null && (supplyQty.toDoubleOrNull() ?: 0.0) > 0.0,
                    colors = ButtonDefaults.buttonColors(containerColor = sem.success),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("warehouse_fulfill_confirm_button"),
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تامین و کسر از انبار")
                }
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = onRouteToPurchase,
                enabled = isOnline && !isSubmitting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("warehouse_route_purchase_button"),
            ) {
                Icon(Icons.Outlined.ShoppingCartCheckout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ارجاع به پیش‌نویس خرید")
            }

            OutlinedButton(
                onClick = onNotInStock,
                enabled = isOnline && !isSubmitting,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = sem.danger),
                border = BorderStroke(1.dp, sem.danger.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("warehouse_not_in_stock_button"),
            ) {
                Icon(Icons.Outlined.RemoveShoppingCart, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("ثبت عدم موجودی / عدم امکان تامین")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReopenReasonSheet(
    item: WarehouseDoneItemDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LocalSyntelColors.current.warning)
                Spacer(Modifier.width(8.dp))
                Text("بازگشایی «${item.name}»", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "با بازگشایی، موجودی کسرشده با ردیف جبرانی برگردانده می‌شود و قلم مجدداً در صف فعال قرار می‌گیرد.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("دلیل بازگشایی (الزامی)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reopen_reason_input"),
            )
            Button(
                onClick = { onConfirm(reason.trim()) },
                enabled = !isSubmitting && reason.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reopen_confirm_button"),
            ) {
                Text("تایید بازگشایی")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
