package io.github.stivengjekaj.zettastream.ui.components

import io.github.stivengjekaj.zettastream.ui.theme.Surface
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.OnAccent
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary

/**
 * A button in the black and white style. On a TV, only the focused button is
 * white, so the focus is clear. On a phone, the primary button is white.
 */
@Composable
fun ZButton(
    onClick: () -> Unit,
    tv: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val filled = if (tv) focused else primary
    Button(
        onClick = onClick,
        shape = Corner,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (filled) Accent else SurfaceHigh,
            contentColor = if (filled) OnAccent else TextPrimary,
        ),
        border = if (filled) null else BorderStroke(1.dp, Outline),
        modifier = modifier.focusRing(Corner, scaleTo = 1.04f),
        content = content,
    )
}

/** A filter chip. The chosen chip is white, as on Miruro 2.0. The others have a thin frame. */
@Composable
fun ZChip(text: String, selected: Boolean, tv: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilterChip(
        shape = Corner,
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = if (tv) 15.sp else 14.sp) },
        modifier = modifier.focusRing(Corner, scaleTo = 1.04f),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Accent, selectedLabelColor = OnAccent,
            containerColor = Surface, labelColor = TextSecondary,
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Outline, selectedBorderColor = Accent),
    )
}
