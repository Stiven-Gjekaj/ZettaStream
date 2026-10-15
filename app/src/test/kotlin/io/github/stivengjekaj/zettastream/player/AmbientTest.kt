package io.github.stivengjekaj.zettastream.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientTest {
    @Test
    fun onlyAVideoOfAnotherShapeNeedsTheFill() {
        assertTrue(Ambient.needed(640, 480, 1f, 1920, 1080))
        assertTrue(Ambient.needed(1920, 800, 1f, 1920, 1080))
        assertFalse(Ambient.needed(1920, 1080, 1f, 1920, 1080))
        assertFalse(Ambient.needed(1280, 720, 1f, 3840, 2160))
        // A DVD picture of 720 x 480 with wide pixels is 16:9 on the screen.
        assertFalse(Ambient.needed(720, 480, 32f / 27f, 1920, 1080))
        assertFalse(Ambient.needed(0, 0, 1f, 1920, 1080))
    }

    @Test
    fun theBlurSpreadsAPointAndKeepsAFlatColour() {
        val flat = IntArray(16) { 0xFF336699.toInt() }
        assertEquals(flat.toList(), Ambient.blur(flat, 4, 4, 1).toList())
        val point = IntArray(25) { 0xFF000000.toInt() }.also { it[12] = 0xFFFFFFFF.toInt() }
        val out = Ambient.blur(point, 5, 5, 1)
        assertTrue(out[12] and 0xFF in 1..254)
        assertTrue(out[11] and 0xFF > 0)
        assertEquals(0xFF, out[0] ushr 24)
    }

    @Test
    fun theChangeIsTheAverageDifferenceOfTheColours() {
        val a = IntArray(4) { 0xFF102030.toInt() }
        assertEquals(0, Ambient.change(a, a.copyOf()))
        assertEquals(10, Ambient.change(a, IntArray(4) { 0xFF1A2A3A.toInt() }))
        assertEquals(Int.MAX_VALUE, Ambient.change(a, IntArray(2)))
    }
}
