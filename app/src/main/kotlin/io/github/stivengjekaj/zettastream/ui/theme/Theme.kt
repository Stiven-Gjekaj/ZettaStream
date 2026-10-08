package io.github.stivengjekaj.zettastream.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.stivengjekaj.zettastream.R

// A black and white palette. Red is only for errors.
val Background = Color(0xFF000000)
val Surface = Color(0xFF0E0E0E)
val SurfaceHigh = Color(0xFF181818)
val Outline = Color(0xFF2E2E2E)
val Accent = Color(0xFFFFFFFF)
val OnAccent = Color(0xFF000000)
val TextPrimary = Color(0xFFF2F2F2)
val TextSecondary = Color(0xFF8C8C8C)
val Danger = Color(0xFFFF6B6B)

/** One corner radius for every card, button, chip, and focus ring. */
val Corner = RoundedCornerShape(8.dp)

private fun mono(weight: FontWeight) = Font(
    R.font.jetbrains_mono,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Mono = FontFamily(
    mono(FontWeight.Light),
    mono(FontWeight.Normal),
    mono(FontWeight.Medium),
    mono(FontWeight.SemiBold),
    mono(FontWeight.Bold),
)

private fun Typography.withFont(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f(), headlineMedium = headlineMedium.f(), headlineSmall = headlineSmall.f(),
        titleLarge = titleLarge.f(), titleMedium = titleMedium.f(), titleSmall = titleSmall.f(),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f(), labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

@Composable
fun ZettaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = OnAccent,
            secondary = TextSecondary,
            background = Background,
            onBackground = TextPrimary,
            surface = Surface,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceHigh,
            onSurfaceVariant = TextSecondary,
            surfaceContainerHigh = SurfaceHigh,
            outline = Outline,
            error = Danger,
        ),
        typography = Typography().withFont(Mono),
        content = content,
    )
}
