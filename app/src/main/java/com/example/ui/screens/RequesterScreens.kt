package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.remote.CreateRequestItemPayload
import com.example.data.remote.ProductCatalogDto
import com.example.data.remote.PurchaseRequestDto
import com.example.data.remote.ReceivedItemDto
import com.example.ui.components.BadgeTone
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiTile
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.SyntelHeaderCard
import com.example.ui.components.SyntelSearchBar
import com.example.ui.components.requestStatusTone
import com.example.ui.theme.LocalSyntelColors
import com.example.viewmodel.RequesterUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun RequesterDashboardScreen(
    session: UserSession,
    state: RequesterUiState,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onOpenNewRequest: () -> Unit,
    onOpenRequests: () -> Unit,
    onOpenReceived: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
                title = "سلام، ${session.fullName.ifBlank { session.username }}",
                subtitle = "بخش: ${session.departmentName.ifBlank { session.roleDisplay }} • ${session.cafeName}",
                badgeText = "پنل درخواست‌دهنده",
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
            )
        }

        if (state.summary.receiptGateBlocked || state.summary.receivedPendingCount > 0) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = sem.dangerContainer),
                    border = BorderStroke(1.dp, sem.danger.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = sem.danger,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${state.summary.receivedPendingCount} قلم منتظر تایید دریافت",
                                style = MaterialTheme.typography.titleMedium,
                                color = sem.danger,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = state.summary.receiptGateMessage.ifBlank {
                                "برای ثبت درخواست جدید، ابتدا وصول کالاهای تحویل‌گرفته‌شده از انبار را تایید کنید."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurface,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onOpenReceived,
                            colors = ButtonDefaults.buttonColors(containerColor = sem.danger),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("go_to_received_gate_button"),
                        ) {
                            Text("مشاهده و تایید کالاهای دریافتی")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiTile(
                    label = "در انتظار بررسی",
                    value = state.summary.pendingRequests.toString(),
                    icon = Icons.Outlined.HourglassTop,
                    tone = BadgeTone.WARNING,
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    label = "تکمیل‌شده",
                    value = state.summary.completedRequests.toString(),
                    icon = Icons.Outlined.CheckCircle,
                    tone = BadgeTone.SUCCESS,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KpiTile(
                    label = "کل درخواست‌ها",
                    value = state.summary.totalRequests.toString(),
                    icon = Icons.Outlined.ReceiptLong,
                    tone = BadgeTone.PRIMARY,
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    label = "دریافت تاییدنشده",
                    value = state.summary.receivedPendingCount.toString(),
                    icon = Icons.Outlined.Inventory2,
                    tone = if (state.summary.receivedPendingCount > 0) BadgeTone.DANGER else BadgeTone.INFO,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onOpenNewRequest,
                    enabled = isOnline && !state.summary.receiptGateBlocked,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("dashboard_new_request_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("ثبت درخواست جدید")
                }
                OutlinedButton(
                    onClick = onOpenRequests,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("dashboard_all_requests_button"),
                ) {
                    Text("لیست درخواست‌ها")
                }
            }
        }

        if (state.requests.isNotEmpty()) {
            item {
                Text(
                    text = "آخرین درخواست‌های من",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface,
                )
            }
            items(state.requests.take(4), key = { it.id }) { req ->
                RequesterRequestCard(
                    request = req,
                    isOnline = isOnline,
                    onEdit = null,
                )
            }
        } else {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "هنوز درخواستی ثبت نشده است",
                    description = "با دکمه «ثبت درخواست جدید» می‌توانید اقلام مورد نیاز بخش خود را مستقیم به انبار ارسال کنید.",
                )
            }
        }
    }
}

@Composable
fun RequesterRequestsScreen(
    state: RequesterUiState,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onEditRequest: (PurchaseRequestDto) -> Unit,
) {
    val filtered = remember(state.requests, state.statusFilter, state.searchQuery) {
        state.requests.filter { req ->
            val matchStatus = when (state.statusFilter) {
                "all" -> true
                "pending" -> req.status in listOf("pending", "pending_manager_approval", "partial")
                "purchased" -> req.status == "purchased"
                "rejected" -> req.status == "rejected"
                else -> true
            }
            val q = state.searchQuery.trim()
            val matchQuery = q.isEmpty() ||
                req.requestNumber.contains(q, ignoreCase = true) ||
                req.items.any { it.name.contains(q, ignoreCase = true) }
            matchStatus && matchQuery
        }
    }

    val filters = listOf(
        "all" to "همه (${state.requests.size})",
        "pending" to "در جریان",
        "purchased" to "تکمیل‌شده",
        "rejected" to "ردشده",
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
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
            SyntelSearchBar(
                query = state.searchQuery,
                onQueryChange = onSearchChange,
                placeholder = "جستجو با شماره درخواست یا نام کالا...",
                testTag = "requester_search_input",
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filters.forEach { (key, label) ->
                    FilterChip(
                        selected = state.statusFilter == key,
                        onClick = { onFilterChange(key) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("requester_filter_$key"),
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.ReceiptLong,
                    title = "درخواستی با این فیلتر پیدا نشد",
                    description = if (isOnline) "فیلتر را تغییر دهید یا درخواست جدید ثبت کنید." else "فقط درخواست‌های کش‌شده نمایش داده می‌شوند.",
                )
            }
        } else {
            items(filtered, key = { it.id }) { req ->
                RequesterRequestCard(
                    request = req,
                    isOnline = isOnline,
                    onEdit = if (req.canEdit) ({ onEditRequest(req) }) else null,
                )
            }
        }
    }
}

@Composable
private fun RequesterRequestCard(
    request: PurchaseRequestDto,
    isOnline: Boolean,
    onEdit: (() -> Unit)?,
) {
    var expanded by remember(request.id) { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, sem.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("request_card_${request.id}"),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "درخواست #${request.requestNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        color = cs.onSurface,
                    )
                    Text(
                        text = request.createdAtJalali.ifBlank { request.createdDateIso },
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    label = request.statusLabel,
                    tone = requestStatusTone(request.status),
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${request.items.size} قلم کالا" +
                        if (request.deliveryDateJalali.isNotBlank()) " • تحویل: ${request.deliveryDateJalali}"
                        else if (request.deliveryImmediate) " • تحویل فوری" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "نمایش اقلام",
                    tint = cs.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = sem.cardBorder)
                    Spacer(Modifier.height(8.dp))
                    request.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (item.description.isNotBlank()) {
                                    Text(
                                        text = item.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "${item.quantity} ${item.unit}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cs.primary,
                                )
                                StatusBadge(
                                    label = item.supplyStatusLabel,
                                    tone = requestStatusTone(item.supplyStatus),
                                )
                            }
                        }
                    }
                    if (request.rejectionReason.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            color = sem.dangerContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "دلیل رد: ${request.rejectionReason}",
                                color = sem.danger,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp),
                            )
                        }
                    }
                    if (onEdit != null) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onEdit,
                            enabled = isOnline,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_request_button_${request.id}"),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ویرایش درخواست")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequesterReceivedScreen(
    state: RequesterUiState,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onConfirmOne: (Int) -> Unit,
    onConfirmAll: () -> Unit,
) {
    var showConfirmed by remember { mutableStateOf(false) }
    val pending = remember(state.receivedItems) { state.receivedItems.filter { !it.isConfirmed } }
    val confirmed = remember(state.receivedItems) { state.receivedItems.filter { it.isConfirmed } }
    val list = if (showConfirmed) confirmed else pending

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showConfirmed,
                    onClick = { showConfirmed = false },
                    label = { Text("منتظر تایید (${pending.size})") },
                    modifier = Modifier.testTag("received_tab_pending"),
                )
                FilterChip(
                    selected = showConfirmed,
                    onClick = { showConfirmed = true },
                    label = { Text("تاییدشده‌ها (${confirmed.size})") },
                    modifier = Modifier.testTag("received_tab_confirmed"),
                )
            }
        }

        if (!showConfirmed && pending.isNotEmpty()) {
            item {
                Button(
                    onClick = onConfirmAll,
                    enabled = isOnline && !state.isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = sem.success),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_all_received_button"),
                ) {
                    Icon(Icons.Rounded.DoneAll, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("تایید دریافت همه (${pending.size} قلم)")
                }
            }
        }

        if (list.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Inventory2,
                    title = if (showConfirmed) "هنوز قلم تاییدشده‌ای نیست" else "همه کالاهای دریافتی تایید شده‌اند",
                    description = "اقلامی که مسئول انبار به بخش شما تحویل می‌دهد در اینجا برای اعلام وصول نمایش داده می‌شوند.",
                )
            }
        } else {
            items(list, key = { it.itemId }) { item ->
                ReceivedItemCard(
                    item = item,
                    isOnline = isOnline,
                    isSubmitting = state.isSubmitting,
                    onConfirm = { onConfirmOne(item.itemId) },
                )
            }
        }
    }
}

@Composable
private fun ReceivedItemCard(
    item: ReceivedItemDto,
    isOnline: Boolean,
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, sem.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "درخواست #${item.requestNumber} • درخواستی: ${item.requestedQty} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    label = if (item.isConfirmed) "تایید شده" else "منتظر تایید",
                    tone = if (item.isConfirmed) BadgeTone.SUCCESS else BadgeTone.WARNING,
                )
            }
            Spacer(Modifier.height(8.dp))
            item.deliveries.forEach { del ->
                Text(
                    text = "• تحویل از ${del.warehouse.ifBlank { "انبار" }}: ${del.qty} ${del.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (item.isConfirmed && item.confirmedAtJalali.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "زمان تایید وصول: ${item.confirmedAtJalali}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
            if (!item.isConfirmed) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onConfirm,
                    enabled = isOnline && !isSubmitting,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_received_button_${item.itemId}"),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تایید دریافت کالا")
                }
            }
        }
    }
}

private data class DraftLineInput(
    val productId: Int? = null,
    val name: String = "",
    val quantity: String = "1",
    val unit: String = "عدد",
    val description: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditRequestSheet(
    editingRequest: PurchaseRequestDto?,
    products: List<ProductCatalogDto>,
    units: List<String>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (deliveryDateIso: String, deliveryImmediate: Boolean, items: List<CreateRequestItemPayload>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lines = remember(editingRequest) {
        mutableStateListOf<DraftLineInput>().apply {
            if (editingRequest != null && editingRequest.items.isNotEmpty()) {
                editingRequest.items.forEach {
                    add(DraftLineInput(it.productId, it.name, it.quantity, it.unit, it.description))
                }
            } else {
                add(DraftLineInput(unit = units.firstOrNull() ?: "عدد"))
            }
        }
    }
    var deliveryImmediate by remember { mutableStateOf(editingRequest?.deliveryImmediate ?: false) }
    var selectedDayOffset by remember { mutableStateOf(0) }
    var activePickerLineIndex by remember { mutableStateOf<Int?>(null) }
    var catalogQuery by remember { mutableStateOf("") }

    val isoFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val dayChoices = remember {
        (0..6).map { offset ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, offset) }
            val label = when (offset) {
                0 -> "امروز"
                1 -> "فردا"
                else -> "+$offset روز"
            }
            offset to (label to isoFmt.format(cal.time))
        }
    }
    val selectedDateIso = dayChoices.firstOrNull { it.first == selectedDayOffset }?.second?.second.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (editingRequest == null) "ثبت درخواست کالا" else "ویرایش درخواست #${editingRequest.requestNumber}",
                style = MaterialTheme.typography.titleLarge,
            )

            // Delivery urgency / date selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("تحویل فوری", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = deliveryImmediate,
                    onCheckedChange = { deliveryImmediate = it },
                    modifier = Modifier.testTag("request_immediate_switch"),
                )
            }

            if (!deliveryImmediate) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayChoices.forEach { (offset, pair) ->
                        FilterChip(
                            selected = selectedDayOffset == offset,
                            onClick = { selectedDayOffset = offset },
                            label = { Text(pair.first) },
                        )
                    }
                }
            }

            // Item rows
            lines.forEachIndexed { idx, line ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("قلم ${idx + 1}", style = MaterialTheme.typography.labelLarge)
                            if (lines.size > 1) {
                                IconButton(
                                    onClick = { lines.removeAt(idx) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "حذف ردیف",
                                        tint = LocalSyntelColors.current.danger,
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = line.name,
                            onValueChange = { txt ->
                                lines[idx] = line.copy(name = txt, productId = null)
                                catalogQuery = txt
                                activePickerLineIndex = idx
                            },
                            label = { Text("نام کالا (جستجو از فهرست)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("request_item_name_$idx"),
                        )

                        if (activePickerLineIndex == idx && products.isNotEmpty()) {
                            val suggestions = products.filter {
                                !it.isBlocked && (catalogQuery.isBlank() || it.name.contains(catalogQuery, ignoreCase = true))
                            }.take(5)
                            if (suggestions.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp),
                                ) {
                                    suggestions.forEach { prod ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    lines[idx] = line.copy(
                                                        productId = prod.id,
                                                        name = prod.name,
                                                        unit = prod.unitName.ifBlank { line.unit },
                                                    )
                                                    activePickerLineIndex = null
                                                },
                                            color = MaterialTheme.colorScheme.surface,
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(prod.name, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    "${prod.unitName} • ${prod.categoryName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = line.quantity,
                                onValueChange = { lines[idx] = line.copy(quantity = it) },
                                label = { Text("مقدار") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("request_item_qty_$idx"),
                            )
                            OutlinedTextField(
                                value = line.unit,
                                onValueChange = { lines[idx] = line.copy(unit = it) },
                                label = { Text("واحد") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        OutlinedTextField(
                            value = line.description,
                            onValueChange = { lines[idx] = line.copy(description = it) },
                            label = { Text("توضیحات (اختیاری)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            TextButton(
                onClick = { lines.add(DraftLineInput(unit = units.firstOrNull() ?: "عدد")) },
                modifier = Modifier.testTag("request_add_line_button"),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("افزودن قلم دیگر")
            }

            Button(
                onClick = {
                    val validItems = lines.filter { it.name.isNotBlank() && (it.quantity.toDoubleOrNull() ?: 0.0) > 0.0 }
                        .map {
                            CreateRequestItemPayload(
                                productId = it.productId,
                                name = it.name.trim(),
                                quantity = it.quantity.trim(),
                                unit = it.unit.ifBlank { "عدد" },
                                description = it.description.trim(),
                            )
                        }
                    if (validItems.isNotEmpty()) {
                        onSubmit(selectedDateIso, deliveryImmediate, validItems)
                    }
                },
                enabled = !isSubmitting && lines.any { it.name.isNotBlank() && (it.quantity.toDoubleOrNull() ?: 0.0) > 0.0 },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("request_submit_button"),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(if (editingRequest == null) "ارسال درخواست به انبار" else "ذخیره تغییرات")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
