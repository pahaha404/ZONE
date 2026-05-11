package com.zone.android.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ZoneColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF050505),
    primaryContainer = Color(0xFF19191D),
    onPrimaryContainer = Color(0xFFF7F7FB),
    secondary = Color(0xFFD8DCE8),
    onSecondary = Color(0xFF090A0D),
    secondaryContainer = Color(0xFF15161B),
    onSecondaryContainer = Color(0xFFE7EAF4),
    tertiary = Color(0xFFC7CCD8),
    onTertiary = Color(0xFF090A0C),
    tertiaryContainer = Color(0xFF15161A),
    onTertiaryContainer = Color(0xFFF0F1F6),
    background = Color(0xFF030303),
    onBackground = Color(0xFFF7F7FB),
    surface = Color(0xFF0B0B0E),
    onSurface = Color(0xFFF5F5F8),
    surfaceVariant = Color(0xFF131317),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFF2A2B31),
    outlineVariant = Color(0xFF1A1B20),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF220705),
    errorContainer = Color(0xFF2A100E),
    onErrorContainer = Color(0xFFFFDAD5),
)

private val ZoneTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 58.sp,
        letterSpacing = (-1.3).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.0).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

private val ZoneShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(40.dp),
)

/**
 * App-wide Compose theme for ZONE.
 */
@Composable
fun ZoneTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ZoneColors,
        typography = ZoneTypography,
        shapes = ZoneShapes,
        content = content,
    )
}
