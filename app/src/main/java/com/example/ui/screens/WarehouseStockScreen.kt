package com.example.ui.screens

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.remote.CreateVoucherItemPayload
import com.example.data.remote.CreateVoucherPayload
import com.example.data.remote.ProductCatalogDto
import com.example.data.remote.StockItemDto
import com.example.data.remote.StockTransactionsResponseDto
import com.example.data.remote.VoucherDto
import com.example.data.remote.WarehouseOptionDto
import com.example.ui.components.BadgeTone
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.SyntelSearchBar
import com.example.ui.components.requestStatusTone
import com.example.ui.theme.LocalSyntelColors
import com.example.viewmodel.WarehouseUiState

@Composable
fun WarehouseStockScreen(
    state: WarehouseUiState,
    selectedStockTx: StockTransactionsResponseDto?,
    loadingTx: Boolean,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onSelectWarehouseFilter: (Int?) -> Unit,
    onToggleLowStock: () -> Unit,
    onSearchChange: (String) -> Unit,
    onOpenStockTransactions: (Int) -> Unit,
    onDismissStockTransactions: () -> Unit,
    onCreateVoucher: (CreateVoucherPayload) -> Unit,
) {
    var activeSegment by remember { mutableIntStateOf(0) } // 0 = stock, 1 = vouchers
    var showCreateVoucherSheet by remember { mutableStateOf(false) }

    val filteredStock = remember(
        state.stockItems,
        state.stockWarehouseFilterId,
        state.lowStockOnly,
        state.stockSearchQuery,
    ) {
        val q = state.stockSearchQuery.trim()
        state.stockItems.filter { item ->
            val matchWh = state.stockWarehouseFilterId == null || item.warehouseId == state.stockWarehouseFilterId
            val matchLow = !state.lowStockOnly || item.isLowStock
            val matchQuery = q.isEmpty() ||
                item.productName.contains(q, ignoreCase = true) ||
                item.categoryName.contains(q, ignoreCase = true) ||
                item.warehouseName.contains(q, ignoreCase = true)
            matchWh && matchLow && matchQuery
        }
    }

    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
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
                        text = if (activeSegment == 0) "موجودی انبارها" else "حواله‌های انبار",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (activeSegment == 0) {
                            "${filteredStock.size} کالا در نمایش فعلی"
                        } else {
                            "${state.vouchers.size} حواله ثبت‌شده"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("stock_refresh_button"),
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Rounded.Sync, contentDescription = "به‌روزرسانی موجودی")
                    }
                }
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeSegment == 0,
                    onClick = { activeSegment = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("segment_stock_tab"),
                ) {
                    Text("موجودی انبار")
                }
                SegmentedButton(
                    selected = activeSegment == 1,
                    onClick = { activeSegment = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("segment_vouchers_tab"),
                ) {
                    Text("حواله‌ها")
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

        if (activeSegment == 0) {
            item {
                SyntelSearchBar(
                    query = state.stockSearchQuery,
                    onQueryChange = onSearchChange,
                    placeholder = "جستجوی نام کالا، دسته‌بندی یا انبار...",
                    testTag = "stock_search_input",
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = state.lowStockOnly,
                        onClick = onToggleLowStock,
                        label = { Text("کمبود موجودی") },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.testTag("low_stock_filter_chip"),
                    )
                    FilterChip(
                        selected = state.stockWarehouseFilterId == null,
                        onClick = { onSelectWarehouseFilter(null) },
                        label = { Text("همه انبارها") },
                        modifier = Modifier.testTag("warehouse_filter_all"),
                    )
                    state.warehouses.forEach { wh ->
                        FilterChip(
                            selected = state.stockWarehouseFilterId == wh.id,
                            onClick = { onSelectWarehouseFilter(wh.id) },
                            label = { Text(wh.name) },
                        )
                    }
                }
            }

            if (filteredStock.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Inventory2,
                        title = "کالایی یافت نشد",
                        description = "فیلترهای موجودی یا عبارت جستجو را بررسی کنید یا لیست را همگام‌سازی نمایید.",
                    )
                }
            } else {
                items(filteredStock, key = { it.id }) { stock ->
                    StockItemCard(
                        item = stock,
                        onClick = { onOpenStockTransactions(stock.id) },
                    )
                }
            }
        } else {
            item {
                Button(
                    onClick = { showCreateVoucherSheet = true },
                    enabled = isOnline && !state.isSubmitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_create_voucher_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("ثبت حواله جدید (ورود، خروج، ضایعات، انتقال)")
                }
            }

            if (state.vouchers.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Description,
                        title = "حواله‌ای ثبت نشده است",
                        description = "حواله‌های ورود، خروج، ضایعات و انتقال بین انبارها در این بخش نمایش داده می‌شوند.",
                    )
                }
            } else {
                items(state.vouchers, key = { it.id }) { voucher ->
                    VoucherCard(voucher = voucher)
                }
            }
        }
    }

    if (loadingTx || selectedStockTx != null) {
        StockTransactionsSheet(
            data = selectedStockTx,
            isLoading = loadingTx,
            onDismiss = onDismissStockTransactions,
        )
    }

    if (showCreateVoucherSheet) {
        CreateVoucherSheet(
            warehouses = state.warehouses,
            products = state.products,
            isSubmitting = state.isSubmitting,
            onDismiss = { showCreateVoucherSheet = false },
            onSubmit = { payload ->
                onCreateVoucher(payload)
                showCreateVoucherSheet = false
            },
        )
    }
}

@Composable
private fun StockItemCard(
    item: StockItemDto,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("stock_item_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isLowStock) sem.danger.copy(alpha = 0.45f) else sem.cardBorder,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${item.warehouseName} • ${item.categoryName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                StatusBadge(
                    label = if (item.isLowStock) "کمبود" else "عادی",
                    tone = if (item.isLowStock) BadgeTone.DANGER else BadgeTone.SUCCESS,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "موجودی: ${item.quantity} ${item.unitName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.isLowStock) sem.danger else cs.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "حداقل: ${item.minQuantity}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (item.lastUpdatedJalali.isNotBlank()) "بروزرسانی: ${item.lastUpdatedJalali}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.History,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "مشاهده گردش کالا",
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoucherCard(voucher: VoucherDto) {
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current
    val typeTone = when (voucher.voucherType) {
        "receipt" -> BadgeTone.SUCCESS
        "issue" -> BadgeTone.INFO
        "waste" -> BadgeTone.DANGER
        "transfer" -> BadgeTone.PRIMARY
        else -> BadgeTone.NEUTRAL
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voucher_card_${voucher.id}"),
        shape = RoundedCornerShape(16.dp),
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
                    text = voucher.voucherNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(label = voucher.voucherTypeLabel, tone = typeTone)
                    StatusBadge(label = voucher.statusLabel, tone = requestStatusTone(voucher.status))
                }
            }
            val routeText = buildString {
                if (voucher.sourceWarehouse.isNotBlank()) append("مبدا: ${voucher.sourceWarehouse}")
                if (voucher.sourceWarehouse.isNotBlank() && voucher.destinationWarehouse.isNotBlank()) append("  ←  ")
                if (voucher.destinationWarehouse.isNotBlank()) append("مقصد: ${voucher.destinationWarehouse}")
            }
            if (routeText.isNotBlank()) {
                Text(
                    text = routeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            if (voucher.itemsSummary.isNotBlank()) {
                Text(
                    text = voucher.itemsSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurface,
                )
            }
            if (voucher.description.isNotBlank()) {
                Text(
                    text = voucher.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${voucher.itemCount} قلم کالا",
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.primary,
                )
                Text(
                    text = voucher.dateJalali,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StockTransactionsSheet(
    data: StockTransactionsResponseDto?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = data?.productName?.ifBlank { "گردش موجودی کالا" } ?: "گردش موجودی کالا",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (data != null) {
                Text(
                    text = "${data.warehouseName} • موجودی فعلی: ${data.currentQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (data == null || data.transactions.isEmpty()) {
                Text(
                    text = "تراکنشی برای این کالا ثبت نشده است.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(data.transactions, key = { it.id }) { tx ->
                        Surface(
                            color = cs.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = tx.typeLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "${tx.previousQuantity} ← ${tx.newQuantity} (${tx.quantity})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = cs.primary,
                                    )
                                }
                                if (tx.description.isNotBlank()) {
                                    Text(
                                        text = tx.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${tx.createdBy} • ${tx.createdAtJalali}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private data class VoucherLineDraft(
    val productId: Int? = null,
    val productName: String = "",
    val quantity: String = "1",
    val description: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateVoucherSheet(
    warehouses: List<WarehouseOptionDto>,
    products: List<ProductCatalogDto>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CreateVoucherPayload) -> Unit,
) {
    val voucherTypes = listOf(
        "receipt" to "رسید ورود",
        "issue" to "حواله خروج",
        "waste" to "ثبت ضایعات",
        "transfer" to "انتقال بین انبار",
    )
    var selectedType by remember { mutableStateOf("receipt") }
    var sourceWhId by remember { mutableStateOf(warehouses.firstOrNull()?.id) }
    var destWhId by remember { mutableStateOf(warehouses.firstOrNull()?.id) }
    var description by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<VoucherLineDraft>() }

    val needsSource = selectedType in listOf("issue", "waste", "transfer")
    val needsDest = selectedType in listOf("receipt", "transfer")

    val filteredProducts = remember(products, productSearch) {
        val q = productSearch.trim()
        if (q.isEmpty()) products.take(12)
        else products.filter { it.name.contains(q, ignoreCase = true) }.take(12)
    }

    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "ثبت حواله جدید انبار",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                voucherTypes.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedType == key,
                        onClick = { selectedType = key },
                        label = { Text(label) },
                        modifier = Modifier.testTag("voucher_type_$key"),
                    )
                }
            }

            if (needsSource) {
                Text("انبار مبدا:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    warehouses.forEach { wh ->
                        FilterChip(
                            selected = sourceWhId == wh.id,
                            onClick = { sourceWhId = wh.id },
                            label = { Text(wh.name) },
                        )
                    }
                }
            }

            if (needsDest) {
                Text("انبار مقصد:", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    warehouses.forEach { wh ->
                        FilterChip(
                            selected = destWhId == wh.id,
                            onClick = { destWhId = wh.id },
                            label = { Text(wh.name) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات حواله (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            HorizontalDivider()
            Text("انتخاب کالا برای افزودن به حواله:", style = MaterialTheme.typography.labelLarge)
            SyntelSearchBar(
                query = productSearch,
                onQueryChange = { productSearch = it },
                placeholder = "نام کالا را جستجو کنید...",
                testTag = "voucher_product_search",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filteredProducts.forEach { product ->
                    OutlinedButton(
                        onClick = {
                            if (lines.none { it.productId == product.id }) {
                                lines.add(VoucherLineDraft(product.id, product.name, "1"))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(product.name)
                    }
                }
            }

            lines.forEachIndexed { idx, line ->
                Surface(
                    color = cs.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = line.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = line.quantity,
                            onValueChange = { v -> lines[idx] = line.copy(quantity = v) },
                            label = { Text("مقدار") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(96.dp),
                        )
                        IconButton(
                            onClick = { lines.removeAt(idx) },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف ردیف", tint = cs.error)
                        }
                    }
                }
            }

            val validItems = lines.mapNotNull { line ->
                val pid = line.productId ?: return@mapNotNull null
                val qty = line.quantity.trim()
                if (qty.isBlank() || (qty.toDoubleOrNull() ?: 0.0) <= 0.0) return@mapNotNull null
                CreateVoucherItemPayload(productId = pid, quantity = qty, description = line.description)
            }
            val validWarehouses = (!needsSource || sourceWhId != null) &&
                (!needsDest || destWhId != null) &&
                (selectedType != "transfer" || sourceWhId != destWhId)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("انصراف")
                }
                Button(
                    onClick = {
                        onSubmit(
                            CreateVoucherPayload(
                                voucherType = selectedType,
                                sourceWarehouseId = if (needsSource) sourceWhId else null,
                                destinationWarehouseId = if (needsDest) destWhId else null,
                                description = description.trim(),
                                items = validItems,
                            )
                        )
                    },
                    enabled = !isSubmitting && validItems.isNotEmpty() && validWarehouses,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.6f)
                        .testTag("submit_voucher_button"),
                ) {
                    Text("ثبت نهایی حواله")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
