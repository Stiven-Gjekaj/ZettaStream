package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import kotlinx.coroutines.launch

/**
 * Makes the focused item larger and puts a ring around it, so that the user
 * sees the focus from across the room. Put it before `clickable`, and give it
 * the same shape as the item, so that the ring follows the edge of the item.
 *
 * It is one modifier node, not a composed modifier, so that a screen with
 * many cards does less work when it builds and when the focus moves.
 */
fun Modifier.focusRing(
    shape: Shape = Corner,
    scaleTo: Float = 1.06f,
    onFocus: () -> Unit = {},
    rememberForReturn: Boolean = true,
): Modifier = this then FocusRingElement(shape, scaleTo, onFocus, rememberForReturn)

private data class FocusRingElement(
    val shape: Shape,
    val scaleTo: Float,
    val onFocus: () -> Unit,
    val rememberForReturn: Boolean,
) : ModifierNodeElement<FocusRingNode>() {
    override fun create() = FocusRingNode(shape, scaleTo, onFocus, rememberForReturn)

    override fun update(node: FocusRingNode) {
        node.shape = shape
        node.scaleTo = scaleTo
        node.onFocus = onFocus
        node.rememberForReturn = rememberForReturn
        node.invalidateDraw()
    }
}

private class FocusRingNode(
    var shape: Shape,
    var scaleTo: Float,
    var onFocus: () -> Unit,
    var rememberForReturn: Boolean,
) : Modifier.Node(), FocusEventModifierNode, FocusRequesterModifierNode, LayoutModifierNode, DrawModifierNode {
    // A draw that reads a state runs again when the state changes.
    private var focused by mutableStateOf(false)
    private val scale = Animatable(1f)

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.isFocused == focused) return
        focused = focusState.isFocused
        if (focused) {
            if (rememberForReturn) FocusReturn.last = { requestFocus() }
            onFocus()
        }
        val target = if (focused) scaleTo else 1f
        coroutineScope.launch { scale.animateTo(target) }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        // The layer reads the scale, so only the layer changes while the scale moves.
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!focused) return
        // The ring is inside the edge of the item, as a border is.
        val width = RING_WIDTH.toPx()
        val inner = Size(size.width - width, size.height - width)
        translate(width / 2, width / 2) {
            drawOutline(shape.createOutline(inner, layoutDirection, this), Accent, style = Stroke(width))
        }
    }
}

private val RING_WIDTH = 2.dp

/**
 * The last item of a screen that had the focus. When the focus leaves the
 * icon bar, it goes back to this item, also when the item is far from the bar.
 */
object FocusReturn {
    /** Gives the focus to the item. It is false when the item is gone. */
    var last: (() -> Boolean)? = null

    /** Gives the focus back. False when the item is gone, for example after a scroll. */
    fun restore(): Boolean = runCatching { last?.invoke() == true }.getOrDefault(false)
}
