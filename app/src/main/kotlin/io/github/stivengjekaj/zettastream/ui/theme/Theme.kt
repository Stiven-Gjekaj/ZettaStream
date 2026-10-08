package io.github.stivengjekaj.zettastream.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Background = Color(0xFF0B0D12)
val Surface = Color(0xFF151823)
val SurfaceHigh = Color(0xFF1E2230)
val Accent = Color(0xFF7C5CFF)
val AccentLight = Color(0xFF9D7DFF)
val Cyan = Color(0xFF4FD1FF)
val TextPrimary = Color(0xFFE8EAF0)
val TextSecondary = Color(0xFFA9ADBC)
val Danger = Color(0xFFFF6B6B)

@Composable
fun ZettaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            onPrimary = Color.White,
            secondary = Cyan,
            background = Background,
            onBackground = TextPrimary,
            surface = Surface,
            onSurface = TextPrimary,
            surfaceVariant = SurfaceHigh,
            onSurfaceVariant = TextSecondary,
            error = Danger,
        ),
        content = content,
    )
}
