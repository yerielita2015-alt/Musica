package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.prefs.AppFontChoice

val SpaceGroteskFontFamily = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.SemiBold),
    Font(R.font.space_grotesk, FontWeight.Bold)
)

val PlusJakartaSansFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildSonoraTypography(
    scale: Float = 1f,
    fontChoice: AppFontChoice = AppFontChoice.MODERN_GROTESK
): Typography {
    fun s(v: Float) = (v * scale).sp
    val displayFamily = when (fontChoice) {
        AppFontChoice.MODERN_GROTESK -> SpaceGroteskFontFamily
        AppFontChoice.GEOMETRIC_SANS -> PlusJakartaSansFontFamily
        AppFontChoice.EDITORIAL_SERIF -> FontFamily.Serif
        AppFontChoice.TECHNICAL_MONO -> JetBrainsMonoFontFamily
    }
    val bodyFamily = when (fontChoice) {
        AppFontChoice.MODERN_GROTESK -> PlusJakartaSansFontFamily
        AppFontChoice.GEOMETRIC_SANS -> PlusJakartaSansFontFamily
        AppFontChoice.EDITORIAL_SERIF -> FontFamily.Serif
        AppFontChoice.TECHNICAL_MONO -> JetBrainsMonoFontFamily
    }

    return Typography(
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(32f),
            lineHeight = s(38f),
            letterSpacing = (-0.5).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = s(24f),
            lineHeight = s(30f),
            letterSpacing = (-0.25).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(20f),
            lineHeight = s(26f)
        ),
        titleLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(18f),
            lineHeight = s(24f)
        ),
        titleMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(15f),
            lineHeight = s(20f)
        ),
        titleSmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = s(13f),
            lineHeight = s(18f)
        ),
        bodyLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(15f),
            lineHeight = s(22f)
        ),
        bodyMedium = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(13.5f),
            lineHeight = s(19f)
        ),
        bodySmall = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.Normal,
            fontSize = s(12f),
            lineHeight = s(16f)
        ),
        labelLarge = TextStyle(
            fontFamily = bodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = s(13f),
            lineHeight = s(18f)
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = s(11.5f),
            lineHeight = s(15f)
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = s(10f),
            lineHeight = s(14f)
        )
    )
}
