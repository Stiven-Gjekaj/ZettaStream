package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.RoundedCornerShape
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
import io.github.stivengjekaj.zettastream.ui.theme.Accent

/**
 * Makes the focused item larger and puts a ring around it, so that the user
 * sees the focus from across the room. Put it before `clickable`.
 */
fun Modifier.focusRing(
    shape: Shape = RoundedCornerShape(12.dp),
    scaleTo: Float = 1.06f,
    onFocus: () -> Unit = {},
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val left = remember { floatArrayOf(Float.MAX_VALUE) }
    val scale by animateFloatAsState(if (focused) scaleTo else 1f, label = "focus scale")
    this
        .onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) {
                FocusTracker.left = left[0]
                onFocus()
            }
        }
        .onGloballyPositioned {
            left[0] = it.boundsInWindow().left
            if (focused) FocusTracker.left = left[0]
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .border(3.dp, if (focused) Accent else Color.Transparent, shape)
}


/** Remembers where the focused item is, so that Left at the left edge can open the menu. */
object FocusTracker {
    var left: Float = Float.MAX_VALUE
}
