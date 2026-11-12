package com.sebas.musicplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Identidad propia: superficies casi negras con un punto de azul y un violeta como acento.
// Los colores de ventana en res/values*/themes.xml deben coincidir con `background`.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA594FF),
    onPrimary = Color(0xFF1B0F5C),
    primaryContainer = Color(0xFF3B2D9E),
    onPrimaryContainer = Color(0xFFE5DFFF),
    inversePrimary = Color(0xFF5B45E0),
    secondary = Color(0xFFC6BFE8),
    onSecondary = Color(0xFF2E2A4A),
    secondaryContainer = Color(0xFF2F2B45),
    onSecondaryContainer = Color(0xFFE4DFFF),
    tertiary = Color(0xFFFF9DB8),
    onTertiary = Color(0xFF5C1130),
    tertiaryContainer = Color(0xFF7A2947),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF0F0F14),
    onBackground = Color(0xFFECEBF2),
    surface = Color(0xFF0F0F14),
    onSurface = Color(0xFFECEBF2),
    surfaceVariant = Color(0xFF2A2A35),
    onSurfaceVariant = Color(0xFFA9A7B8),
    surfaceTint = Color(0xFFA594FF),
    inverseSurface = Color(0xFFECEBF2),
    inverseOnSurface = Color(0xFF2C2C37),
    outline = Color(0xFF5D5B6C),
    outlineVariant = Color(0xFF34333F),
    surfaceBright = Color(0xFF373743),
    surfaceDim = Color(0xFF0F0F14),
    surfaceContainerLowest = Color(0xFF0A0A0E),
    surfaceContainerLow = Color(0xFF15151B),
    surfaceContainer = Color(0xFF1A1A21),
    surfaceContainerHigh = Color(0xFF22222B),
    surfaceContainerHighest = Color(0xFF2C2C37),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B45E0),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5DFFF),
    onPrimaryContainer = Color(0xFF1A0A66),
    inversePrimary = Color(0xFFA594FF),
    secondary = Color(0xFF5F5A7D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE6E0FA),
    onSecondaryContainer = Color(0xFF1C1836),
    tertiary = Color(0xFFB3305C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3F001C),
    background = Color(0xFFFBFAFF),
    onBackground = Color(0xFF1A1924),
    surface = Color(0xFFFBFAFF),
    onSurface = Color(0xFF1A1924),
    surfaceVariant = Color(0xFFE6E3F0),
    onSurfaceVariant = Color(0xFF5E5C6E),
    surfaceTint = Color(0xFF5B45E0),
    inverseSurface = Color(0xFF2F2E3A),
    inverseOnSurface = Color(0xFFF3F1FA),
    outline = Color(0xFF7B7990),
    outlineVariant = Color(0xFFCBC8D8),
    surfaceBright = Color(0xFFFBFAFF),
    surfaceDim = Color(0xFFDBD9E4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F3FB),
    surfaceContainer = Color(0xFFEFEDF7),
    surfaceContainerHigh = Color(0xFFE9E7F2),
    surfaceContainerHighest = Color(0xFFE3E1EC),
)

private val Base = Typography()

// Misma fuente del sistema, pero con jerarquía marcada: títulos con peso, cuerpo ligero.
private val AppTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = Base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.copy(letterSpacing = 0.1.sp),
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * [dynamicColor] (colores del fondo de pantalla, Android 12+) queda apagado por defecto para que
 * la app tenga su propia identidad; se puede encender si algún día hay una pantalla de ajustes.
 */
@Composable
fun MusicPlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
