package io.github.stivengjekaj.zettastream.diag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DiagnosticsTest {
    @Before fun reset() {
        Diagnostics.toLogcat = false
        Diagnostics.clear()
    }

    @Test
    fun aLineHoldsTheTagTheMessageAndTheReason() {
        Diagnostics.log("player", "error at 1000", IllegalStateException("no reader"))
        val line = Diagnostics.lines().single()
        assertTrue(line, line.endsWith("player: error at 1000 (IllegalStateException: no reader)"))
    }

    @Test
    fun onlyTheLastLinesStay() {
        repeat(Diagnostics.MAX_LINES + 20) { Diagnostics.log("t", "line $it") }
        val lines = Diagnostics.lines()
        assertEquals(Diagnostics.MAX_LINES, lines.size)
        assertTrue(lines.last().endsWith("line ${Diagnostics.MAX_LINES + 19}"))
    }
}
