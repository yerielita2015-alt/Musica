package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Material Design 3 Dark Surface Roles (Baseline M3 Dark)
val M3DarkBg = Color(0xFF141218)
val M3DarkSurface = Color(0xFF141218)
val M3DarkSurfaceContainerLowest = Color(0xFF0F0D13)
val M3DarkSurfaceContainerLow = Color(0xFF1D1B20)
val M3DarkSurfaceContainer = Color(0xFF211F26)
val M3DarkSurfaceContainerHigh = Color(0xFF2B2930)
val M3DarkSurfaceContainerHighest = Color(0xFF36343B)
val M3DarkOnSurface = Color(0xFFE6E0E9)
val M3DarkOnSurfaceVariant = Color(0xFFCAC4D0)
val M3DarkOutline = Color(0xFF938F99)
val M3DarkOutlineVariant = Color(0xFF49454F)

// AMOLED Pure Black M3 Surface Roles
val M3AmoledBg = Color(0xFF000000)
val M3AmoledSurfaceLow = Color(0xFF111114)
val M3AmoledSurface = Color(0xFF17171B)
val M3AmoledSurfaceHigh = Color(0xFF202026)

// Material Design 3 Light Surface Roles
val M3LightBg = Color(0xFFFEF7FF)
val M3LightSurface = Color(0xFFFEF7FF)
val M3LightSurfaceContainerLow = Color(0xFFF7F2FA)
val M3LightSurfaceContainer = Color(0xFFF3EDF7)
val M3LightSurfaceContainerHigh = Color(0xFFECE6F0)
val M3LightOnSurface = Color(0xFF1D1B20)
val M3LightOnSurfaceVariant = Color(0xFF49454F)
val M3LightOutline = Color(0xFF79747E)
val M3LightOutlineVariant = Color(0xFFCAC4D0)

// Curated Material 3 Tonal Palettes
data class M3TonalPalette(
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val lightOnPrimaryContainer: Color,
    val lightSecondary: Color,
    val lightSecondaryContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkPrimaryContainer: Color,
    val darkOnPrimaryContainer: Color,
    val darkSecondary: Color,
    val darkSecondaryContainer: Color
)

val IndigoM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF4C57A9),
    lightPrimaryContainer = Color(0xFFDFE0FF),
    lightOnPrimaryContainer = Color(0xFF000C61),
    lightSecondary = Color(0xFF5B5D72),
    lightSecondaryContainer = Color(0xFFE0E1F9),
    darkPrimary = Color(0xFFBCC2FF),
    darkOnPrimary = Color(0xFF1B2678),
    darkPrimaryContainer = Color(0xFF343F90),
    darkOnPrimaryContainer = Color(0xFFDFE0FF),
    darkSecondary = Color(0xFFC4C5DD),
    darkSecondaryContainer = Color(0xFF434659)
)

val CobaltM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF1B5FB8),
    lightPrimaryContainer = Color(0xFFD6E3FF),
    lightOnPrimaryContainer = Color(0xFF001B3E),
    lightSecondary = Color(0xFF555F71),
    lightSecondaryContainer = Color(0xFFD9E3F8),
    darkPrimary = Color(0xFFA8C7FA),
    darkOnPrimary = Color(0xFF003063),
    darkPrimaryContainer = Color(0xFF00468C),
    darkOnPrimaryContainer = Color(0xFFD6E3FF),
    darkSecondary = Color(0xFFBDC7DC),
    darkSecondaryContainer = Color(0xFF3E4758)
)

val SageM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF3D6838),
    lightPrimaryContainer = Color(0xFFBEF0B2),
    lightOnPrimaryContainer = Color(0xFF002203),
    lightSecondary = Color(0xFF53634F),
    lightSecondaryContainer = Color(0xFFD6E8CE),
    darkPrimary = Color(0xFFA3D398),
    darkOnPrimary = Color(0xFF0D380E),
    darkPrimaryContainer = Color(0xFF265022),
    darkOnPrimaryContainer = Color(0xFFBEF0B2),
    darkSecondary = Color(0xFFBACBB3),
    darkSecondaryContainer = Color(0xFF3C4B38)
)

val EmeraldM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF146C53),
    lightPrimaryContainer = Color(0xFFA5F2D4),
    lightOnPrimaryContainer = Color(0xFF002117),
    lightSecondary = Color(0xFF4C635A),
    lightSecondaryContainer = Color(0xFFCEE9DD),
    darkPrimary = Color(0xFF7FD9B8),
    darkOnPrimary = Color(0xFF003829),
    darkPrimaryContainer = Color(0xFF00513D),
    darkOnPrimaryContainer = Color(0xFFA5F2D4),
    darkSecondary = Color(0xFFB3CCC1),
    darkSecondaryContainer = Color(0xFF354B43)
)

val OceanM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF006684),
    lightPrimaryContainer = Color(0xFFBCE9FF),
    lightOnPrimaryContainer = Color(0xFF001F2A),
    lightSecondary = Color(0xFF4C616B),
    lightSecondaryContainer = Color(0xFFCFE6F1),
    darkPrimary = Color(0xFF69D3F8),
    darkOnPrimary = Color(0xFF003546),
    darkPrimaryContainer = Color(0xFF004D64),
    darkOnPrimaryContainer = Color(0xFFBCE9FF),
    darkSecondary = Color(0xFFB3CAD5),
    darkSecondaryContainer = Color(0xFF354A53)
)

val AmberM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF7B5800),
    lightPrimaryContainer = Color(0xFFFFDEA0),
    lightOnPrimaryContainer = Color(0xFF271900),
    lightSecondary = Color(0xFF6C5C3F),
    lightSecondaryContainer = Color(0xFFF6E0BB),
    darkPrimary = Color(0xFFF3C66F),
    darkOnPrimary = Color(0xFF412D00),
    darkPrimaryContainer = Color(0xFF5D4200),
    darkOnPrimaryContainer = Color(0xFFFFDEA0),
    darkSecondary = Color(0xFFD9C4A0),
    darkSecondaryContainer = Color(0xFF53452A)
)

val TerracottaM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF984426),
    lightPrimaryContainer = Color(0xFFFFDBD0),
    lightOnPrimaryContainer = Color(0xFF390C00),
    lightSecondary = Color(0xFF77574D),
    lightSecondaryContainer = Color(0xFFFFDBD0),
    darkPrimary = Color(0xFFFFB59D),
    darkOnPrimary = Color(0xFF5B1B03),
    darkPrimaryContainer = Color(0xFF7A2E12),
    darkOnPrimaryContainer = Color(0xFFFFDBD0),
    darkSecondary = Color(0xFFE7BDB0),
    darkSecondaryContainer = Color(0xFF5D4036)
)

val RoseM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF984061),
    lightPrimaryContainer = Color(0xFFFFD9E2),
    lightOnPrimaryContainer = Color(0xFF3E001D),
    lightSecondary = Color(0xFF74565F),
    lightSecondaryContainer = Color(0xFFFFD9E2),
    darkPrimary = Color(0xFFFFB1C8),
    darkOnPrimary = Color(0xFF5E1133),
    darkPrimaryContainer = Color(0xFF7B2949),
    darkOnPrimaryContainer = Color(0xFFFFD9E2),
    darkSecondary = Color(0xFFE3BDC6),
    darkSecondaryContainer = Color(0xFF5A3F47)
)

val PlumM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF6750A4),
    lightPrimaryContainer = Color(0xFFEADDFF),
    lightOnPrimaryContainer = Color(0xFF21005D),
    lightSecondary = Color(0xFF625B71),
    lightSecondaryContainer = Color(0xFFE8DEF8),
    darkPrimary = Color(0xFFD0BCFF),
    darkOnPrimary = Color(0xFF381E72),
    darkPrimaryContainer = Color(0xFF4F378B),
    darkOnPrimaryContainer = Color(0xFFEADDFF),
    darkSecondary = Color(0xFFCCC2DC),
    darkSecondaryContainer = Color(0xFF4A4458)
)

val MonochromeM3Palette = M3TonalPalette(
    lightPrimary = Color(0xFF5D5E61),
    lightPrimaryContainer = Color(0xFFE2E2E6),
    lightOnPrimaryContainer = Color(0xFF1A1C1E),
    lightSecondary = Color(0xFF5E5E62),
    lightSecondaryContainer = Color(0xFFE3E2E6),
    darkPrimary = Color(0xFFC6C6CA),
    darkOnPrimary = Color(0xFF2F3033),
    darkPrimaryContainer = Color(0xFF45474A),
    darkOnPrimaryContainer = Color(0xFFE2E2E6),
    darkSecondary = Color(0xFFC7C6CA),
    darkSecondaryContainer = Color(0xFF46464A)
)
