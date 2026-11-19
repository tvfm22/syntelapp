package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalSyntelColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BadgeTone {
    SUCCESS, WARNING, DANGER, INFO, PRIMARY, NEUTRAL
}

@Composable
fun StatusBadge(
    label: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val sem = LocalSyntelColors.current
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        BadgeTone.SUCCESS -> sem.successContainer to sem.success
        BadgeTone.WARNING -> sem.warningContainer to sem.warning
        BadgeTone.DANGER -> sem.dangerContainer to sem.danger
        BadgeTone.INFO -> sem.infoContainer to sem.info
        BadgeTone.PRIMARY -> cs.primaryContainer to cs.primary
        BadgeTone.NEUTRAL -> cs.surfaceVariant to cs.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

fun requestStatusTone(status: String): BadgeTone = when (status) {
    "purchased", "fulfilled", "approved_from_stock", "finalized" -> BadgeTone.SUCCESS
    "pending_manager_approval", "pending", "draft", "partial" -> BadgeTone.WARNING
    "rejected", "not_in_stock", "not_purchased", "cancelled", "declined" -> BadgeTone.DANGER
    "returned", "sent", "approved_purchase", "from_stock" -> BadgeTone.INFO
    else -> BadgeTone.NEUTRAL
}

@Composable
fun OfflineStatusBanner(
    isOnline: Boolean,
    lastSyncedMillis: Long?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sem = LocalSyntelColors.current
    AnimatedVisibility(
        visible = !isOnline,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            color = sem.warningContainer,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, sem.warning.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag("offline_status_banner"),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = stringResource(R.string.cd_offline_banner),
                    tint = sem.warning,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "حالت آفلاین — نمایش حافظه موقت",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = lastSyncedMillis?.let {
                            "آخرین همگام‌سازی: ${formatSyncTime(it)} • ثبت تغییرات نیازمند اینترنت است"
                        } ?: "بدون اتصال به سرور؛ لیست‌های ذخیره‌شده قابل مشاهده هستند",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("offline_retry_button"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh),
                        tint = sem.warning,
                    )
                }
            }
        }
    }
}

@Composable
fun SyntelHeaderCard(
    title: String,
    subtitle: String,
    badgeText: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val sem = LocalSyntelColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(sem.gradientStart, sem.gradientEnd)
                )
            )
            .padding(18.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (!badgeText.isNullOrBlank()) {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                text = badgeText,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (onRefresh != null) {
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = CircleShape,
                    ) {
                        IconButton(
                            onClick = onRefresh,
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("header_refresh_button"),
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Sync,
                                    contentDescription = stringResource(R.string.cd_refresh),
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }
            }
            if (bottomContent != null) {
                Spacer(Modifier.height(14.dp))
                bottomContent()
            }
        }
    }
}

@Composable
fun KpiTile(
    label: String,
    value: String,
    icon: ImageVector,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val sem = LocalSyntelColors.current
    val cs = MaterialTheme.colorScheme
    val accent = when (tone) {
        BadgeTone.SUCCESS -> sem.success
        BadgeTone.WARNING -> sem.warning
        BadgeTone.DANGER -> sem.danger
        BadgeTone.INFO -> sem.info
        BadgeTone.PRIMARY -> cs.primary
        BadgeTone.NEUTRAL -> cs.onSurfaceVariant
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, sem.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SyntelSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String = "search_input",
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = stringResource(R.string.cd_search),
                tint = cs.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.minimumInteractiveComponentSize(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = cs.onSurfaceVariant,
                    )
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cs.surface,
            unfocusedContainerColor = cs.surface,
            focusedBorderColor = cs.primary,
            unfocusedBorderColor = LocalSyntelColors.current.cardBorder,
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, LocalSyntelColors.current.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = cs.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun QuantityStepper(
    value: Double,
    onValueChange: (Double) -> Unit,
    unitLabel: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, LocalSyntelColors.current.cardBorder, RoundedCornerShape(12.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { if (value > 1.0) onValueChange((value - 1.0).coerceAtLeast(0.5)) },
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "کاهش تعداد", tint = cs.primary)
        }
        Text(
            text = "${formatQty(value)} $unitLabel",
            style = MaterialTheme.typography.titleSmall,
            color = cs.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        IconButton(
            onClick = { onValueChange(value + 1.0) },
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "افزایش تعداد", tint = cs.primary)
        }
    }
}

fun formatQty(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}

fun formatSyncTime(millis: Long): String {
    return try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    } catch (_: Exception) {
        ""
    }
}
