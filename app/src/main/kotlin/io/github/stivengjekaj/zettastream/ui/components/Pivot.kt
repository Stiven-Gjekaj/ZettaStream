package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Scrolls so that the focused item always stops at the same place. The
 * default scrolls only as far as necessary, so the row moves by a different
 * amount at each press and seems to wobble.
 */
@OptIn(ExperimentalFoundationApi::class)
class PivotSpec(private val pivotPx: Float, private val fraction: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        offset - (containerSize * fraction + pivotPx)
}

/** Keeps the focused item at [pivot] from the start of the scroll area, plus [fraction] of its size. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PivotScroll(pivot: Dp, fraction: Float = 0f, content: @Composable () -> Unit) {
    val px = with(LocalDensity.current) { pivot.toPx() }
    val spec = remember(px, fraction) { PivotSpec(px, fraction) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

/** On a TV, a row of items stops each focused item at the left margin. A phone keeps the default. */
@Composable
fun TvRow(tv: Boolean, content: @Composable () -> Unit) {
    if (tv) PivotScroll(Sizes.gutter(true), content = content) else content()
}
