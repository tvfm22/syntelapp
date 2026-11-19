package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCartCheckout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FactCheck
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCartCheckout
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppWorkspace
import com.example.data.remote.PurchaseRequestDto
import com.example.data.repository.SyntelRepository
import com.example.ui.screens.CreateEditRequestSheet
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.PurchasingDashboardScreen
import com.example.ui.screens.PurchasingHistoryScreen
import com.example.ui.screens.RequesterDashboardScreen
import com.example.ui.screens.RequesterReceivedScreen
import com.example.ui.screens.RequesterRequestsScreen
import com.example.ui.screens.WarehouseDraftScreen
import com.example.ui.screens.WarehouseQueueScreen
import com.example.ui.screens.WarehouseStockScreen
import com.example.ui.theme.SyntelTheme
import com.example.viewmodel.PurchasingViewModel
import com.example.viewmodel.RequesterViewModel
import com.example.viewmodel.SyntelAppViewModel
import com.example.viewmodel.WarehouseViewModel

private data class NavTabItem(
    val key: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0,
)

@Composable
fun SyntelAppRoot(appViewModel: SyntelAppViewModel) {
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val isOnline by appViewModel.isOnline.collectAsStateWithLifecycle()
    val loginState by appViewModel.loginState.collectAsStateWithLifecycle()
    val snackbarMessage by appViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        appViewModel.consumeMessage()
    }

    SyntelTheme(darkTheme = session.darkTheme) {
        if (!session.isLoggedIn) {
            LoginScreen(
                state = loginState,
                isOnline = isOnline,
                onLogin = { user, pass, cafe -> appViewModel.login(user, pass, cafe) },
                onClearError = { appViewModel.clearLoginError() },
            )
            return@SyntelTheme
        }

        val requesterVm: RequesterViewModel = viewModel(
            factory = remember(appViewModel.repository) { RequesterViewModel.factory(appViewModel.repository) }
        )
        val warehouseVm: WarehouseViewModel = viewModel(
            factory = remember(appViewModel.repository) { WarehouseViewModel.factory(appViewModel.repository) }
        )
        val purchasingVm: PurchasingViewModel = viewModel(
            factory = remember(appViewModel.repository) { PurchasingViewModel.factory(appViewModel.repository) }
        )

        val requesterState by requesterVm.uiState.collectAsStateWithLifecycle()
        val warehouseState by warehouseVm.uiState.collectAsStateWithLifecycle()
        val selectedStockTx by warehouseVm.selectedStockTx.collectAsStateWithLifecycle()
        val loadingTx by warehouseVm.loadingTx.collectAsStateWithLifecycle()
        val purchasingState by purchasingVm.uiState.collectAsStateWithLifecycle()

        var selectedTab by rememberSaveable(session.activeWorkspace) { mutableIntStateOf(0) }
        var showRequestSheet by remember { mutableStateOf(false) }
        var editingRequest by remember { mutableStateOf<PurchaseRequestDto?>(null) }
        var showSelfBuyHistory by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(session.cafeCode, session.activeWorkspace, isOnline) {
            if (!isOnline) return@LaunchedEffect
            when (session.activeWorkspace) {
                AppWorkspace.REQUESTER -> requesterVm.refresh()
                AppWorkspace.WAREHOUSE -> {
                    warehouseVm.refreshAll()
                    purchasingVm.setScope(SyntelRepository.SCOPE_WAREHOUSE_SELF)
                }
                AppWorkspace.PURCHASING -> {
                    purchasingVm.setScope(SyntelRepository.SCOPE_PURCHASING)
                }
            }
        }

        LaunchedEffect(session.token, isOnline) {
            if (isOnline && session.isLoggedIn) {
                appViewModel.refreshProfile()
            }
        }

        val tabs = remember(
            session.activeWorkspace,
            requesterState.summary.receivedPendingCount,
            warehouseState.meta.draftCount,
            warehouseState.meta.myPurchasesCount,
            purchasingState.meta.totalItems,
        ) {
            when (session.activeWorkspace) {
                AppWorkspace.REQUESTER -> listOf(
                    NavTabItem("req_home", "پیشخوان", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
                    NavTabItem("req_list", "درخواست‌ها", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong),
                    NavTabItem(
                        "req_received",
                        "دریافتی‌ها",
                        Icons.Rounded.FactCheck,
                        Icons.Outlined.FactCheck,
                        badgeCount = requesterState.summary.receivedPendingCount,
                    ),
                    NavTabItem("profile", "پروفایل", Icons.Rounded.Person, Icons.Outlined.Person),
                )

                AppWorkspace.WAREHOUSE -> listOf(
                    NavTabItem("wh_queue", "صف انبار", Icons.Rounded.Inventory2, Icons.Outlined.Inventory2),
                    NavTabItem(
                        "wh_draft",
                        "پیش‌نویس",
                        Icons.Rounded.ShoppingCartCheckout,
                        Icons.Outlined.ShoppingCartCheckout,
                        badgeCount = warehouseState.draftItems.size.takeIf { it > 0 } ?: warehouseState.meta.draftCount,
                    ),
                    NavTabItem("wh_stock", "انبار و حواله", Icons.Rounded.Inventory, Icons.Outlined.Inventory),
                    NavTabItem(
                        "wh_self_buy",
                        "خرید من",
                        Icons.Rounded.ShoppingBag,
                        Icons.Outlined.ShoppingBag,
                        badgeCount = warehouseState.meta.myPurchasesCount,
                    ),
                    NavTabItem("profile", "پروفایل", Icons.Rounded.Person, Icons.Outlined.Person),
                )

                AppWorkspace.PURCHASING -> listOf(
                    NavTabItem(
                        "pur_list",
                        "لیست خرید",
                        Icons.Rounded.ShoppingBag,
                        Icons.Outlined.ShoppingBag,
                        badgeCount = purchasingState.meta.totalItems,
                    ),
                    NavTabItem("pur_history", "سوابق خرید", Icons.Rounded.History, Icons.Outlined.History),
                    NavTabItem("profile", "پروفایل", Icons.Rounded.Person, Icons.Outlined.Person),
                )
            }
        }

        val safeTabIndex = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))

        BackHandler(enabled = showSelfBuyHistory || safeTabIndex != 0) {
            if (showSelfBuyHistory) {
                showSelfBuyHistory = false
            } else {
                selectedTab = 0
            }
        }

        val showRequesterFab = session.activeWorkspace == AppWorkspace.REQUESTER &&
            (safeTabIndex == 0 || safeTabIndex == 1)

        val openCreateRequest = {
            if (requesterState.summary.receiptGateBlocked) {
                appViewModel.showMessage(
                    requesterState.summary.receiptGateMessage.ifBlank {
                        "ابتدا کالاهای دریافتی خود را در بخش دریافتی‌ها تایید کنید."
                    }
                )
                selectedTab = 2
            } else if (!isOnline) {
                appViewModel.showMessage("برای ثبت درخواست جدید اتصال اینترنت لازم است.")
            } else {
                editingRequest = null
                showRequestSheet = true
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 840.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = safeTabIndex == index
                            NavigationRailItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTab = index
                                    showSelfBuyHistory = false
                                },
                                icon = { TabIconWithBadge(tab, isSelected) },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_rail_${tab.key}"),
                            )
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.weight(1f),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = {
                        if (showRequesterFab) {
                            ExtendedFloatingActionButton(
                                onClick = openCreateRequest,
                                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                                text = { Text("درخواست جدید") },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("new_request_fab"),
                            )
                        }
                    },
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 2.dp,
                            ) {
                                tabs.forEachIndexed { index, tab ->
                                    val isSelected = safeTabIndex == index
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTab = index
                                            showSelfBuyHistory = false
                                        },
                                        icon = { TabIconWithBadge(tab, isSelected) },
                                        label = {
                                            Text(
                                                text = tab.label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_bar_${tab.key}"),
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 760.dp),
                        ) {
                            when (session.activeWorkspace) {
                                AppWorkspace.REQUESTER -> when (safeTabIndex) {
                                    0 -> RequesterDashboardScreen(
                                        session = session,
                                        state = requesterState,
                                        isOnline = isOnline,
                                        onRefresh = { requesterVm.refresh { appViewModel.showMessage(it) } },
                                        onOpenNewRequest = openCreateRequest,
                                        onOpenRequests = { selectedTab = 1 },
                                        onOpenReceived = { selectedTab = 2 },
                                    )

                                    1 -> RequesterRequestsScreen(
                                        state = requesterState,
                                        isOnline = isOnline,
                                        onRefresh = { requesterVm.refresh { appViewModel.showMessage(it) } },
                                        onSearchChange = { requesterVm.setSearchQuery(it) },
                                        onFilterChange = { requesterVm.setStatusFilter(it) },
                                        onEditRequest = { req ->
                                            if (!isOnline) {
                                                appViewModel.showMessage("برای ویرایش درخواست اتصال اینترنت لازم است.")
                                            } else {
                                                editingRequest = req
                                                showRequestSheet = true
                                            }
                                        },
                                    )

                                    2 -> RequesterReceivedScreen(
                                        state = requesterState,
                                        isOnline = isOnline,
                                        onRefresh = { requesterVm.refresh { appViewModel.showMessage(it) } },
                                        onConfirmOne = { itemId ->
                                            requesterVm.confirmReceived(
                                                itemId = itemId,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                        onConfirmAll = {
                                            requesterVm.confirmReceived(
                                                confirmAll = true,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                    )

                                    else -> ProfileScreen(
                                        session = session,
                                        isOnline = isOnline,
                                        onSwitchWorkspace = { appViewModel.switchWorkspace(it) },
                                        onSwitchCafe = { appViewModel.switchCafe(it) },
                                        onToggleDarkTheme = { appViewModel.setDarkTheme(it) },
                                        onRefreshProfile = { appViewModel.refreshProfile() },
                                        onLogout = { appViewModel.logout() },
                                    )
                                }

                                AppWorkspace.WAREHOUSE -> when (safeTabIndex) {
                                    0 -> WarehouseQueueScreen(
                                        session = session,
                                        state = warehouseState,
                                        isOnline = isOnline,
                                        onRefresh = { warehouseVm.refreshAll { appViewModel.showMessage(it) } },
                                        onShiftDay = { delta -> warehouseVm.shiftDay(delta) { appViewModel.showMessage(it) } },
                                        onJumpToday = { warehouseVm.jumpToToday { appViewModel.showMessage(it) } },
                                        onSelectDepartment = { dept ->
                                            warehouseVm.setDepartmentFilter(dept) { appViewModel.showMessage(it) }
                                        },
                                        onFulfill = { itemId, allocations ->
                                            warehouseVm.performItemAction(
                                                itemId = itemId,
                                                action = "fulfill",
                                                allocations = allocations,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                        onRouteToPurchase = { itemId ->
                                            warehouseVm.performItemAction(
                                                itemId = itemId,
                                                action = "to_purchase",
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                        onNotInStock = { itemId ->
                                            warehouseVm.performItemAction(
                                                itemId = itemId,
                                                action = "not_in_stock",
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                        onReopen = { itemId, reason ->
                                            warehouseVm.performItemAction(
                                                itemId = itemId,
                                                action = "reopen",
                                                reason = reason,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                    )

                                    1 -> WarehouseDraftScreen(
                                        state = warehouseState,
                                        isOnline = isOnline,
                                        onRefresh = { warehouseVm.refreshAll { appViewModel.showMessage(it) } },
                                        onAddManualItem = { productId, name, qty, unit, desc ->
                                            warehouseVm.addDraftItem(
                                                productId = productId,
                                                name = name,
                                                quantity = qty,
                                                unit = unit,
                                                description = desc,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                        onUpdateItem = { itemId, qty, desc ->
                                            warehouseVm.updateDraftItem(
                                                itemId = itemId,
                                                quantity = qty,
                                                description = desc,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                        onDeleteItem = { itemId ->
                                            warehouseVm.deleteDraftItem(
                                                itemId = itemId,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                        onSendDraft = { selectedIds, officerId ->
                                            warehouseVm.sendDraft(
                                                selectedIds = selectedIds,
                                                officerUserId = officerId,
                                                onSuccess = { msg ->
                                                    appViewModel.showMessage(msg)
                                                    if (officerId == "self") {
                                                        purchasingVm.setScope(SyntelRepository.SCOPE_WAREHOUSE_SELF)
                                                    }
                                                },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                    )

                                    2 -> WarehouseStockScreen(
                                        state = warehouseState,
                                        selectedStockTx = selectedStockTx,
                                        loadingTx = loadingTx,
                                        isOnline = isOnline,
                                        onRefresh = { warehouseVm.refreshAll { appViewModel.showMessage(it) } },
                                        onSelectWarehouseFilter = { warehouseVm.setStockWarehouseFilter(it) },
                                        onToggleLowStock = { warehouseVm.toggleLowStockOnly() },
                                        onSearchChange = { warehouseVm.setStockSearchQuery(it) },
                                        onOpenStockTransactions = { stockId ->
                                            if (!isOnline) {
                                                appViewModel.showMessage("برای مشاهده گردش کالا اتصال اینترنت لازم است.")
                                            } else {
                                                warehouseVm.loadStockTransactions(stockId) {
                                                    appViewModel.showMessage(it)
                                                }
                                            }
                                        },
                                        onDismissStockTransactions = { warehouseVm.clearStockTransactions() },
                                        onCreateVoucher = { payload ->
                                            warehouseVm.createVoucher(
                                                payload = payload,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                    )

                                    3 -> if (showSelfBuyHistory) {
                                        PurchasingHistoryScreen(
                                            state = purchasingState,
                                            isOnline = isOnline,
                                            onRefresh = {
                                                purchasingVm.setScope(SyntelRepository.SCOPE_WAREHOUSE_SELF) {
                                                    appViewModel.showMessage(it)
                                                }
                                            },
                                            onStatusFilterChange = { purchasingVm.setHistoryStatusFilter(it) },
                                            onSearchChange = { purchasingVm.setSearchQuery(it) },
                                            onBackToList = { showSelfBuyHistory = false },
                                        )
                                    } else {
                                        PurchasingDashboardScreen(
                                            session = session,
                                            state = purchasingState,
                                            isOnline = isOnline,
                                            onRefresh = {
                                                purchasingVm.setScope(SyntelRepository.SCOPE_WAREHOUSE_SELF) {
                                                    appViewModel.showMessage(it)
                                                }
                                            },
                                            onSearchChange = { purchasingVm.setSearchQuery(it) },
                                            onPerformAction = { ids, action, notes ->
                                                purchasingVm.performAction(
                                                    assignmentIds = ids,
                                                    action = action,
                                                    notes = notes,
                                                    onSuccess = { appViewModel.showMessage(it) },
                                                    onError = { appViewModel.showMessage(it) },
                                                )
                                            },
                                            onSwitchToHistory = { showSelfBuyHistory = true },
                                        )
                                    }

                                    else -> ProfileScreen(
                                        session = session,
                                        isOnline = isOnline,
                                        onSwitchWorkspace = { appViewModel.switchWorkspace(it) },
                                        onSwitchCafe = { appViewModel.switchCafe(it) },
                                        onToggleDarkTheme = { appViewModel.setDarkTheme(it) },
                                        onRefreshProfile = { appViewModel.refreshProfile() },
                                        onLogout = { appViewModel.logout() },
                                    )
                                }

                                AppWorkspace.PURCHASING -> when (safeTabIndex) {
                                    0 -> PurchasingDashboardScreen(
                                        session = session,
                                        state = purchasingState,
                                        isOnline = isOnline,
                                        onRefresh = {
                                            purchasingVm.setScope(SyntelRepository.SCOPE_PURCHASING) {
                                                appViewModel.showMessage(it)
                                            }
                                        },
                                        onSearchChange = { purchasingVm.setSearchQuery(it) },
                                        onPerformAction = { ids, action, notes ->
                                            purchasingVm.performAction(
                                                assignmentIds = ids,
                                                action = action,
                                                notes = notes,
                                                onSuccess = { appViewModel.showMessage(it) },
                                                onError = { appViewModel.showMessage(it) },
                                            )
                                        },
                                    )

                                    1 -> PurchasingHistoryScreen(
                                        state = purchasingState,
                                        isOnline = isOnline,
                                        onRefresh = {
                                            purchasingVm.setScope(SyntelRepository.SCOPE_PURCHASING) {
                                                appViewModel.showMessage(it)
                                            }
                                        },
                                        onStatusFilterChange = { purchasingVm.setHistoryStatusFilter(it) },
                                        onSearchChange = { purchasingVm.setSearchQuery(it) },
                                    )

                                    else -> ProfileScreen(
                                        session = session,
                                        isOnline = isOnline,
                                        onSwitchWorkspace = { appViewModel.switchWorkspace(it) },
                                        onSwitchCafe = { appViewModel.switchCafe(it) },
                                        onToggleDarkTheme = { appViewModel.setDarkTheme(it) },
                                        onRefreshProfile = { appViewModel.refreshProfile() },
                                        onLogout = { appViewModel.logout() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showRequestSheet) {
            CreateEditRequestSheet(
                editingRequest = editingRequest,
                products = requesterState.products,
                units = requesterState.units,
                isSubmitting = requesterState.isSubmitting,
                onDismiss = {
                    showRequestSheet = false
                    editingRequest = null
                },
                onSubmit = { deliveryDateIso, immediate, items ->
                    requesterVm.submitRequest(
                        editingRequestId = editingRequest?.id,
                        deliveryDateIso = deliveryDateIso,
                        deliveryImmediate = immediate,
                        items = items,
                        onSuccess = { msg ->
                            showRequestSheet = false
                            editingRequest = null
                            appViewModel.showMessage(msg)
                        },
                        onError = { err -> appViewModel.showMessage(err) },
                    )
                },
            )
        }
    }
}

@Composable
private fun TabIconWithBadge(tab: NavTabItem, selected: Boolean) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab.badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (tab.badgeCount > 99) "99+" else tab.badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tab.label)
        }
    } else {
        Icon(imageVector = icon, contentDescription = tab.label)
    }
}
