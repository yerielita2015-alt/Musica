package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.prefs.AccentTheme
import com.example.data.prefs.AppFontChoice
import com.example.data.prefs.CornerStyle
import com.example.data.prefs.ThemeMode

private fun paletteFor(accent: AccentTheme): M3TonalPalette = when (accent) {
    AccentTheme.INDIGO_TONAL -> IndigoM3Palette
    AccentTheme.COBALT_TONAL -> CobaltM3Palette
    AccentTheme.SAGE_TONAL -> SageM3Palette
    AccentTheme.EMERALD_TONAL -> EmeraldM3Palette
    AccentTheme.OCEAN_TONAL -> OceanM3Palette
    AccentTheme.AMBER_TONAL -> AmberM3Palette
    AccentTheme.TERRACOTTA_TONAL -> TerracottaM3Palette
    AccentTheme.ROSE_TONAL -> RoseM3Palette
    AccentTheme.MONOCHROME_M3 -> MonochromeM3Palette
    AccentTheme.PLUM_TONAL, AccentTheme.DYNAMIC_M3 -> PlumM3Palette
}

@Composable
fun SonoraTheme(
    themeMode: ThemeMode = ThemeMode.DARK_M3,
    accentTheme: AccentTheme = AccentTheme.DYNAMIC_M3,
    fontChoice: AppFontChoice = AppFontChoice.MODERN_GROTESK,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.LIGHT_M3 -> false
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK_M3, ThemeMode.AMOLED_DARK -> true
    }

    val colorScheme: ColorScheme = if (
        accentTheme == AccentTheme.DYNAMIC_M3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        val dynamic = if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        if (themeMode == ThemeMode.AMOLED_DARK && useDark) {
            dynamic.copy(
                background = M3AmoledBg,
                surface = M3AmoledBg,
                surfaceContainerLowest = M3AmoledBg,
                surfaceContainerLow = M3AmoledSurfaceLow,
                surfaceContainer = M3AmoledSurface,
                surfaceContainerHigh = M3AmoledSurfaceHigh
            )
        } else {
            dynamic
        }
    } else {
        val p = paletteFor(accentTheme)
        if (useDark) {
            val isAmoled = themeMode == ThemeMode.AMOLED_DARK
            darkColorScheme(
                primary = p.darkPrimary,
                onPrimary = p.darkOnPrimary,
                primaryContainer = p.darkPrimaryContainer,
                onPrimaryContainer = p.darkOnPrimaryContainer,
                secondary = p.darkSecondary,
                onSecondary = M3DarkBg,
                secondaryContainer = p.darkSecondaryContainer,
                onSecondaryContainer = M3DarkOnSurface,
                tertiary = p.darkPrimary,
                onTertiary = p.darkOnPrimary,
                background = if (isAmoled) M3AmoledBg else M3DarkBg,
                onBackground = M3DarkOnSurface,
                surface = if (isAmoled) M3AmoledBg else M3DarkSurface,
                onSurface = M3DarkOnSurface,
                surfaceVariant = if (isAmoled) M3AmoledSurface else M3DarkSurfaceContainerHigh,
                onSurfaceVariant = M3DarkOnSurfaceVariant,
                surfaceContainerLowest = if (isAmoled) M3AmoledBg else M3DarkSurfaceContainerLowest,
                surfaceContainerLow = if (isAmoled) M3AmoledSurfaceLow else M3DarkSurfaceContainerLow,
                surfaceContainer = if (isAmoled) M3AmoledSurface else M3DarkSurfaceContainer,
                surfaceContainerHigh = if (isAmoled) M3AmoledSurfaceHigh else M3DarkSurfaceContainerHigh,
                surfaceContainerHighest = if (isAmoled) M3AmoledSurfaceHigh else M3DarkSurfaceContainerHighest,
                outline = M3DarkOutline,
                outlineVariant = M3DarkOutlineVariant,
                error = Color(0xFFF2B8B5),
                onError = Color(0xFF601410)
            )
        } else {
            lightColorScheme(
                primary = p.lightPrimary,
                onPrimary = Color.White,
                primaryContainer = p.lightPrimaryContainer,
                onPrimaryContainer = p.lightOnPrimaryContainer,
                secondary = p.lightSecondary,
                onSecondary = Color.White,
                secondaryContainer = p.lightSecondaryContainer,
                onSecondaryContainer = M3LightOnSurface,
                tertiary = p.lightPrimary,
                onTertiary = Color.White,
                background = M3LightBg,
                onBackground = M3LightOnSurface,
                surface = M3LightSurface,
                onSurface = M3LightOnSurface,
                surfaceVariant = M3LightSurfaceContainerHigh,
                onSurfaceVariant = M3LightOnSurfaceVariant,
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = M3LightSurfaceContainerLow,
                surfaceContainer = M3LightSurfaceContainer,
                surfaceContainerHigh = M3LightSurfaceContainerHigh,
                outline = M3LightOutline,
                outlineVariant = M3LightOutlineVariant
            )
        }
    }

    val shapes = when (cornerStyle) {
        CornerStyle.COMPACT -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(10.dp),
            large = RoundedCornerShape(14.dp),
            extraLarge = RoundedCornerShape(20.dp)
        )
        CornerStyle.PILL -> Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        )
        CornerStyle.ROUNDED -> Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(28.dp)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildSonoraTypography(fontScale, fontChoice),
        shapes = shapes,
        content = content
    )
}
