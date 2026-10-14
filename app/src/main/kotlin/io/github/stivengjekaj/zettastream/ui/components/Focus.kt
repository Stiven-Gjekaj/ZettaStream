package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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
    rememberForReturn: Boolean = true,
): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) scaleTo else 1f, label = "focus scale")
    val requester = remember { FocusRequester() }
    this
        .focusRequester(requester)
        .onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) {
                if (rememberForReturn) FocusReturn.last = requester
                onFocus()
            }
        }
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .border(2.dp, if (focused) Accent else Color.Transparent, shape)
}



/**
 * The last item of a screen that had the focus. When the focus leaves the
 * icon bar, it goes back to this item, also when the item is far from the bar.
 */
object FocusReturn {
    var last: FocusRequester? = null

    /** Gives the focus back. False when the item is gone, for example after a scroll. */
    fun restore(): Boolean = runCatching { last?.requestFocus() == true }.getOrDefault(false)
}
