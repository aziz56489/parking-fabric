package com.parking.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Midnight drive": deep indigo night, electric violet headlights, amber parking signs.

private val Indigo950 = Color(0xFF0B0B2B)
private val Indigo900 = Color(0xFF1E1B4B)
private val Indigo700 = Color(0xFF4338CA)
private val Indigo600 = Color(0xFF4F46E5)
private val Indigo300 = Color(0xFFA5B4FC)
private val Indigo100 = Color(0xFFE0E7FF)
private val Violet600 = Color(0xFF7C3AED)
private val Violet400 = Color(0xFFA78BFA)
private val Amber500 = Color(0xFFF59E0B)
private val Amber400 = Color(0xFFFBBF24)
private val Amber100 = Color(0xFFFEF3C7)
private val Emerald500 = Color(0xFF10B981)
private val Emerald400 = Color(0xFF34D399)
private val Rose600 = Color(0xFFE11D48)
private val Rose400 = Color(0xFFFB7185)

private val LightColors = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Violet600,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Color(0xFF451A03),
    error = Rose600,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF4C0519),
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FD),
    surfaceContainer = Color(0xFFF1F3F9),
    surfaceContainerHigh = Color(0xFFEBEEF6),
    surfaceContainerHighest = Color(0xFFE4E8F2),
    outline = Color(0xFFCBD2E1),
    outlineVariant = Color(0xFFE2E8F0),
)

private val DarkColors = darkColorScheme(
    primary = Indigo300,
    onPrimary = Indigo900,
    primaryContainer = Indigo700,
    onPrimaryContainer = Indigo100,
    secondary = Violet400,
    onSecondary = Color(0xFF2E1065),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Amber400,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Amber100,
    error = Rose400,
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFE4E6),
    background = Color(0xFF080816),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF12122A),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1C1C3A),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0B0B1F),
    surfaceContainerLow = Color(0xFF121228),
    surfaceContainer = Color(0xFF17172F),
    surfaceContainerHigh = Color(0xFF1E1E3A),
    surfaceContainerHighest = Color(0xFF262646),
    outline = Color(0xFF3B3B60),
    outlineVariant = Color(0xFF2A2A4A),
)

/** Colours Material 3 has no slot for. */
data class BrandColors(
    val heroGradient: Brush,
    val buttonGradient: Brush,
    val accentGradient: Brush,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val onHero: Color,
    val onHeroMuted: Color,
)

private val LightBrand = BrandColors(
    heroGradient = Brush.linearGradient(listOf(Indigo900, Indigo600, Violet600)),
    buttonGradient = Brush.horizontalGradient(listOf(Indigo600, Violet600)),
    accentGradient = Brush.linearGradient(listOf(Amber500, Color(0xFFF97316))),
    success = Color(0xFF059669),
    warning = Amber500,
    danger = Rose600,
    onHero = Color.White,
    onHeroMuted = Color.White.copy(alpha = 0.72f),
)

private val DarkBrand = BrandColors(
    heroGradient = Brush.linearGradient(listOf(Indigo950, Indigo900, Color(0xFF3B0764))),
    buttonGradient = Brush.horizontalGradient(listOf(Indigo600, Violet600)),
    accentGradient = Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFEA580C))),
    success = Emerald400,
    warning = Amber400,
    danger = Rose400,
    onHero = Color.White,
    onHeroMuted = Color.White.copy(alpha = 0.68f),
)

private val LocalBrand = staticCompositionLocalOf { LightBrand }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
)

/** Small all-caps label used above titles ("CLOSEST VACANT PARKING"). */
val Overline: TextStyle
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.typography.labelSmall

object ParkingTheme {
    val brand: BrandColors
        @Composable @ReadOnlyComposable
        get() = LocalBrand.current
}

@Composable
fun ParkingTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalBrand provides if (dark) DarkBrand else LightBrand) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
