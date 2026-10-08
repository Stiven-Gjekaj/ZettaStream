package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Corner

/**
 * Makes the focused item larger and puts a ring around it, so that the user
 * sees the focus from across the room. Put it before `clickable`, and give it
 * the same shape as the item, so that the ring follows the edge of the item.
 */
fun Modifier.focusRing(
    shape: Shape = Corner,
    scaleTo: Float = 1.06f,
    onFocus: () -> Unit = {},
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) scaleTo else 1f, label = "focus scale")
    this
        .onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocus()
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .border(2.dp, if (focused) Accent else Color.Transparent, shape)
}


