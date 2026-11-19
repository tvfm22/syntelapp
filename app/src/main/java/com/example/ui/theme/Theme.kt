package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
data class SyntelSemanticColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val cardBorder: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
)

private val LightSemanticColors = SyntelSemanticColors(
    success = StatusSuccess,
    successContainer = StatusSuccessContainerLight,
    warning = StatusWarning,
    warningContainer = StatusWarningContainerLight,
    danger = StatusDanger,
    dangerContainer = StatusDangerContainerLight,
    info = StatusInfo,
    infoContainer = StatusInfoContainerLight,
    cardBorder = SyntelBorderLight,
    gradientStart = SyntelPurple,
    gradientEnd = SyntelPurpleDark,
)

private val DarkSemanticColors = SyntelSemanticColors(
    success = Color(0xFF34D399),
    successContainer = StatusSuccessContainerDark,
    warning = Color(0xFFFBBF24),
    warningContainer = StatusWarningContainerDark,
    danger = Color(0xFFF87171),
    dangerContainer = StatusDangerContainerDark,
    info = Color(0xFF60A5FA),
    infoContainer = StatusInfoContainerDark,
    cardBorder = SyntelBorderDark,
    gradientStart = SyntelPurpleDark,
    gradientEnd = SyntelPurpleDeep,
)

val LocalSyntelColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = SyntelPurple,
    onPrimary = Color.White,
    primaryContainer = SyntelPurpleContainer,
    onPrimaryContainer = SyntelOnPurpleContainer,
    secondary = SyntelPink,
    onSecondary = Color.White,
    secondaryContainer = SyntelPinkContainer,
    onSecondaryContainer = SyntelOnPinkContainer,
    tertiary = StatusTeal,
    onTertiary = Color.White,
    background = SyntelBgPageLight,
    onBackground = SyntelTextPrimaryLight,
    surface = SyntelSurfaceLight,
    onSurface = SyntelTextPrimaryLight,
    surfaceVariant = SyntelSurfaceVariantLight,
    onSurfaceVariant = SyntelTextSecondaryLight,
    outline = SyntelBorderStrongLight,
    outlineVariant = SyntelBorderLight,
    error = StatusDanger,
    onError = Color.White,
    errorContainer = StatusDangerContainerLight,
    onErrorContainer = Color(0xFF991B1B),
)

private val DarkColorScheme = darkColorScheme(
    primary = SyntelPurpleLight,
    onPrimary = Color(0xFF1E1038),
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = SyntelPurpleContainer,
    secondary = Color(0xFFF472B6),
    onSecondary = Color(0xFF4A0424),
    secondaryContainer = Color(0xFF831843),
    onSecondaryContainer = SyntelPinkContainer,
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    background = SyntelBgPageDark,
    onBackground = SyntelTextPrimaryDark,
    surface = SyntelSurfaceDark,
    onSurface = SyntelTextPrimaryDark,
    surfaceVariant = SyntelSurfaceVariantDark,
    onSurfaceVariant = SyntelTextSecondaryDark,
    outline = Color(0xFF475569),
    outlineVariant = SyntelBorderDark,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = StatusDangerContainerDark,
    onErrorContainer = StatusDangerContainerLight,
)

private val SyntelShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun SyntelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semantic = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalSyntelColors provides semantic,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = SyntelShapes,
            content = content,
        )
    }
}
