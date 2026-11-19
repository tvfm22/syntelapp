package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.local.AppWorkspace
import com.example.data.local.UserSession
import com.example.data.remote.CafeOptionDto
import com.example.ui.components.BadgeTone
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalSyntelColors

@Composable
fun ProfileScreen(
    session: UserSession,
    isOnline: Boolean,
    onSwitchWorkspace: (AppWorkspace) -> Unit,
    onSwitchCafe: (CafeOptionDto) -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    onRefreshProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val workspaces = remember(session) { session.availableWorkspaces() }
    val cs = MaterialTheme.colorScheme
    val sem = LocalSyntelColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                border = BorderStroke(1.dp, sem.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(sem.gradientStart, sem.gradientEnd))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = "تصویر پروفایل کاربر",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = session.fullName.ifBlank { session.username },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "@${session.username}" +
                                if (session.departmentName.isNotBlank()) " • ${session.departmentName}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusBadge(
                                label = session.roleDisplay.ifBlank { session.activeWorkspace.persianLabel },
                                tone = BadgeTone.PRIMARY,
                            )
                            if (session.cafeName.isNotBlank()) {
                                StatusBadge(label = session.cafeName, tone = BadgeTone.INFO)
                            }
                        }
                    }
                }
            }
        }

        if (workspaces.size > 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    border = BorderStroke(1.dp, sem.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.SwapHoriz,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "تغییر میز کار (نقش فعال)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "حساب شما به چند نقش دسترسی دارد. میز کار مورد نظر را انتخاب کنید:",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            workspaces.forEach { ws ->
                                FilterChip(
                                    selected = session.activeWorkspace == ws,
                                    onClick = { onSwitchWorkspace(ws) },
                                    label = { Text(ws.persianLabel) },
                                    modifier = Modifier.testTag("switch_workspace_${ws.key}"),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (session.availableCafes.size > 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    border = BorderStroke(1.dp, sem.cardBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "انتخاب شعبه فعال",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            session.availableCafes.forEach { cafe ->
                                val selected = cafe.code == session.cafeCode
                                OutlinedButton(
                                    onClick = { if (!selected) onSwitchCafe(cafe) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) cs.primary else sem.cardBorder,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("switch_cafe_${cafe.code}"),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("${cafe.name} (${cafe.code})")
                                        if (selected) {
                                            StatusBadge(label = "فعال", tone = BadgeTone.SUCCESS)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                border = BorderStroke(1.dp, sem.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.DarkMode,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "حالت تیره (Dark Mode)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "مناسب محیط‌های کم‌نور و انبار",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = session.darkTheme,
                            onCheckedChange = onToggleDarkTheme,
                            modifier = Modifier.testTag("dark_theme_switch"),
                        )
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isOnline) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) sem.success else sem.warning,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isOnline) "متصل به سرور Syntel" else "حالت آفلاین (حافظه موقت فعال)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "لیست‌ها به‌صورت خودکار برای مشاهده در زمان قطعی اینترنت ذخیره می‌شوند.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = cs.onSurfaceVariant,
                                )
                            }
                        }
                        TextButton(
                            onClick = onRefreshProfile,
                            enabled = isOnline,
                            modifier = Modifier.testTag("refresh_profile_button"),
                        ) {
                            Text("بروزرسانی")
                        }
                    }

                    HorizontalDivider()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Dns,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "نشانی سرور متصل",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurfaceVariant,
                            )
                            Text(
                                text = BuildConfig.SYNTEL_BASE_URL,
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurface,
                            )
                        }
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, sem.danger.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = sem.danger),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("خروج از حساب کاربری", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("خروج از حساب کاربری") },
            text = { Text("با خروج از حساب، توکن نشست و حافظه موقت این دستگاه پاک می‌شود. آیا مطمئن هستید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = sem.danger, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_logout_button"),
                ) {
                    Text("بله، خروج")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("انصراف")
                }
            },
        )
    }
}
