package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.remote.DraftItemDto
import com.example.data.remote.ProductCatalogDto
import com.example.ui.components.BadgeTone
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalSyntelColors
import com.example.viewmodel.WarehouseUiState

@Composable
fun WarehouseDraftScreen(
    state: WarehouseUiState,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onAddManualItem: (productId: Int?, name: String, qty: String, unit: String, desc: String) -> Unit,
    onUpdateItem: (itemId: Int, qty: String, desc: String) -> Unit,
    onDeleteItem: (itemId: Int) -> Unit,
    onSendDraft: (selectedIds: List<Int>, officerUserId: String) -> Unit,
) {
    val selectedIds = remember { mutableStateListOf<Int>() }
    var selectedOfficerId by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<DraftItemDto?>(null) }

    LaunchedEffect(state.draftItems) {
        val currentIds = state.draftItems.map { it.id }.toSet()
        selectedIds.removeAll { it !in currentIds }
        state.draftItems.forEach { if (it.id !in selectedIds) selectedIds.add(it.id) }
    }
    LaunchedEffect(state.officers) {
        if (selectedOfficerId.isBlank() && state.officers.isNotEmpty()) {
            selectedOfficerId = state.officers.first().id
        }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("پیش‌نویس درخواست خرید", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "اقلام ارجاع‌شده از صف انبار یا افزوده‌شده دستی",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = { showAddSheet = true },
                    enabled = isOnline,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("draft_add_manual_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("افزودن کالا")
                }
            }
        }

        if (state.draftItems.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.ShoppingBag,
                    title = "پیش‌نویس خرید خالی است",
                    description = "کالاهایی که از صف انبار «ارجاع به خرید» شوند یا به صورت دستی اضافه کنید اینجا جمع می‌شوند.",
                )
            }
        } else {
            items(state.draftItems, key = { it.id }) { item ->
                val checked = item.id in selectedIds
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    border = BorderStroke(
                        1.dp,
                        if (item.needsCeo) sem.warning.copy(alpha = 0.6f) else sem.cardBorder,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (item.id !in selectedIds) selectedIds.add(item.id)
                                    } else {
                                        selectedIds.remove(item.id)
                                    }
                                },
                                modifier = Modifier.testTag("draft_checkbox_${item.id}"),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${item.quantity} ${item.unit}" +
                                        if (item.fromRequest) " • ارجاع از درخواست" else " • ثبت دستی",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.primary,
                                )
                            }
                            if (isOnline) {
                                IconButton(
                                    onClick = { editingItem = item },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "ویرایش قلم")
                                }
                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "حذف قلم", tint = sem.danger)
                                }
                            }
                        }

                        if (item.needsCeo) {
                            Spacer(Modifier.height(6.dp))
                            Surface(
                                color = sem.warningContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = sem.warning,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "نیازمند تایید مدیرعامل: ${item.ceoReasons.joinToString("، ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cs.onSurface,
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.height(4.dp))
                            StatusBadge("ارسال مستقیم به واحد خرید", BadgeTone.SUCCESS)
                        }
                    }
                }
            }

            // Officer selector + Send button
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    border = BorderStroke(1.dp, sem.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("مسئول خرید مقصد را انتخاب کنید:", style = MaterialTheme.typography.titleSmall)
                        state.officers.forEach { officer ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOfficerId = officer.id },
                            ) {
                                RadioButton(
                                    selected = selectedOfficerId == officer.id,
                                    onClick = { selectedOfficerId = officer.id },
                                )
                                Text(officer.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onSendDraft(selectedIds.toList(), selectedOfficerId) },
                            enabled = isOnline && !state.isSubmitting && selectedIds.isNotEmpty() && selectedOfficerId.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("draft_send_button"),
                        ) {
                            Icon(Icons.Outlined.Send, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("ارسال ${selectedIds.size} قلم انتخاب‌شده")
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddDraftItemSheet(
            products = state.products,
            isSubmitting = state.isSubmitting,
            onDismiss = { showAddSheet = false },
            onAdd = { pid, name, qty, unit, desc ->
                onAddManualItem(pid, name, qty, unit, desc)
                showAddSheet = false
            },
        )
    }

    editingItem?.let { item ->
        EditDraftItemSheet(
            item = item,
            isSubmitting = state.isSubmitting,
            onDismiss = { editingItem = null },
            onSave = { qty, desc ->
                onUpdateItem(item.id, qty, desc)
                editingItem = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDraftItemSheet(
    products: List<ProductCatalogDto>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onAdd: (productId: Int?, name: String, qty: String, unit: String, desc: String) -> Unit,
) {
    var selectedProductId by remember { mutableStateOf<Int?>(null) }
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("عدد") }
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("افزودن کالا به پیش‌نویس خرید", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    selectedProductId = null
                },
                label = { Text("نام کالا") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("draft_manual_name_input"),
            )
            if (name.isNotBlank() && selectedProductId == null) {
                val matches = products.filter { it.name.contains(name, ignoreCase = true) }.take(4)
                if (matches.isNotEmpty()) {
                    Column(modifier = Modifier.heightIn(max = 150.dp)) {
                        matches.forEach { p ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProductId = p.id
                                        name = p.name
                                        unit = p.unitName.ifBlank { "عدد" }
                                    }
                            ) {
                                Text(
                                    text = "${p.name} (${p.unitName})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("مقدار") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("واحد") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات (اختیاری)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onAdd(selectedProductId, name, quantity, unit, description) },
                enabled = !isSubmitting && name.isNotBlank() && (quantity.toDoubleOrNull() ?: 0.0) > 0.0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("draft_manual_confirm_button"),
            ) {
                Text("افزودن به پیش‌نویس")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDraftItemSheet(
    item: DraftItemDto,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (qty: String, desc: String) -> Unit,
) {
    var quantity by remember(item.id) { mutableStateOf(item.quantity) }
    var description by remember(item.id) { mutableStateOf(item.description) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("ویرایش «${item.name}»", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("مقدار (${item.unit})") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(quantity, description) },
                enabled = !isSubmitting && (quantity.toDoubleOrNull() ?: 0.0) > 0.0,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text("ذخیره تغییرات")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
