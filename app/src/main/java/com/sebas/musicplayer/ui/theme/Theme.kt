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

// Identidad ZM: carbón y vinilo, cian apagado como luz, latón y azul Blue Note como detalles.
// Los colores de ventana en res/values*/colors.xml deben coincidir con `background`.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF5BA3AE),
    onPrimary = Color(0xFF05262B),
    primaryContainer = Color(0xFF1E4F57),
    onPrimaryContainer = Color(0xFFC9E6EA),
    inversePrimary = Color(0xFF2F7480),
    secondary = Color(0xFFD2B57A),
    onSecondary = Color(0xFF3A2A0A),
    secondaryContainer = Color(0xFF3B3320),
    onSecondaryContainer = Color(0xFFF0DFB8),
    tertiary = Color(0xFF7FA9D9),
    onTertiary = Color(0xFF0A2A4D),
    tertiaryContainer = Color(0xFF1E5F9E),
    onTertiaryContainer = Color(0xFFD6E6F7),
    background = Color(0xFF0B0B0C),
    onBackground = Color(0xFFEDE6D3),
    surface = Color(0xFF0B0B0C),
    onSurface = Color(0xFFEDE6D3),
    surfaceVariant = Color(0xFF2A2F34),
    onSurfaceVariant = Color(0xFF9AA0A6),
    surfaceTint = Color(0xFF5BA3AE),
    inverseSurface = Color(0xFFEDE6D3),
    inverseOnSurface = Color(0xFF1B1E21),
    outline = Color(0xFF5A6066),
    outlineVariant = Color(0xFF2A2F34),
    surfaceBright = Color(0xFF2F343A),
    surfaceDim = Color(0xFF0B0B0C),
    surfaceContainerLowest = Color(0xFF070708),
    surfaceContainerLow = Color(0xFF111315),
    surfaceContainer = Color(0xFF131517),
    surfaceContainerHigh = Color(0xFF1B1E21),
    surfaceContainerHighest = Color(0xFF252A2E),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF2F7480),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC9E6EA),
    onPrimaryContainer = Color(0xFF0B2F35),
    inversePrimary = Color(0xFF5BA3AE),
    secondary = Color(0xFF8F6B2A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBDDB8),
    onSecondaryContainer = Color(0xFF3A2A0A),
    tertiary = Color(0xFF1E5F9E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD6E6F7),
    onTertiaryContainer = Color(0xFF0A2A4D),
    background = Color(0xFFEFE7D2),
    onBackground = Color(0xFF14130F),
    surface = Color(0xFFEFE7D2),
    onSurface = Color(0xFF14130F),
    surfaceVariant = Color(0xFFE3D9BE),
    onSurfaceVariant = Color(0xFF5A564A),
    surfaceTint = Color(0xFF2F7480),
    inverseSurface = Color(0xFF2A2822),
    inverseOnSurface = Color(0xFFF6F0DF),
    outline = Color(0xFF8A836F),
    outlineVariant = Color(0xFFD6CCB1),
    surfaceBright = Color(0xFFF6F0DF),
    surfaceDim = Color(0xFFDDD3B8),
    surfaceContainerLowest = Color(0xFFFBF7EA),
    surfaceContainerLow = Color(0xFFF6F0DF),
    surfaceContainer = Color(0xFFF1EAD6),
    surfaceContainerHigh = Color(0xFFEBE3CB),
    surfaceContainerHighest = Color(0xFFE3D9BE),
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
