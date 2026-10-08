package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.ui.components.PivotSpec
import org.junit.Assert.assertEquals
import org.junit.Test

class PivotSpecTest {
    @Test
    fun theFocusedItemGoesToThePivot() {
        val spec = PivotSpec(pivotPx = 96f, fraction = 0f)
        // An item that starts at 400 px moves 304 px, so that it starts at 96 px.
        assertEquals(304f, spec.calculateScrollDistance(400f, 300f, 1920f))
        // The same press always gives the same stop, wherever the item was.
        assertEquals(-50f, spec.calculateScrollDistance(46f, 300f, 1920f))
    }

    @Test
    fun aFractionMovesThePivotDownTheScreen() {
        val spec = PivotSpec(pivotPx = 0f, fraction = 0.2f)
        assertEquals(784f, spec.calculateScrollDistance(1000f, 500f, 1080f))
    }
}
