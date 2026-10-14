package io.github.stivengjekaj.zettastream.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeGuardTest {
    private var now = 10_000L
    private val guard = EdgeGuard { now }

    @Test
    fun aSinglePressCanLeaveTheRow() {
        guard.onLeft(repeat = false)
        assertTrue(guard.allowed)
    }

    @Test
    fun aFastRunOfPressesStopsAtTheEdge() {
        guard.onLeft(repeat = false)
        now += 150
        guard.onLeft(repeat = false)
        assertFalse(guard.allowed)
        now += EdgeGuard.WINDOW
        guard.onLeft(repeat = false)
        assertTrue(guard.allowed)
    }

    @Test
    fun aHeldKeyStopsAtTheEdge() {
        guard.onLeft(repeat = true)
        assertFalse(guard.allowed)
    }
}
