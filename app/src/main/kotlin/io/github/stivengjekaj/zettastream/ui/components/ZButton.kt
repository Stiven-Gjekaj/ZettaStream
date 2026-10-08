package io.github.stivengjekaj.zettastream.ui.components

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
