package io.github.stivengjekaj.zettastream.player

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFiltersTest {
    private fun of(f: VideoFilter, r: Float, g: Float, b: Float) = ColourMatrix.apply(ColourMatrix.of(f)!!, r, g, b)

    @Test
    fun onlyTheColourFiltersHaveAMatrix() {
        assertNull(ColourMatrix.of(VideoFilter.None))
        assertNull(ColourMatrix.of(VideoFilter.Crt))
        VideoFilter.entries.filter { it.colour }.forEach { assertTrue(it.name, ColourMatrix.of(it) != null) }
    }

    @Test
    fun blackAndWhiteHasNoColour() {
        val red = of(VideoFilter.BlackWhite, 1f, 0f, 0f)
        assertEquals(red[0], red[1], 0.0001f)
        assertEquals(red[1], red[2], 0.0001f)
        assertArrayEquals(floatArrayOf(1f, 1f, 1f), of(VideoFilter.BlackWhite, 1f, 1f, 1f), 0.0001f)
    }

    @Test
    fun vintageIsWarmAndFaded() {
        val grey = of(VideoFilter.Vintage, 0.5f, 0.5f, 0.5f)
        assertTrue("red over blue", grey[0] > grey[2] + 0.05f)
        val black = of(VideoFilter.Vintage, 0f, 0f, 0f)
        assertTrue("black is lifted", black.all { it > 0.03f })
        val white = of(VideoFilter.Vintage, 1f, 1f, 1f)
        assertTrue("white is not full", white[2] < 1f)
    }

    @Test
    fun vhsHasLessColourAndAWarmTint() {
        val red = of(VideoFilter.Vhs, 1f, 0f, 0f)
        assertTrue("red spills into green", red[1] > 0.05f)
        val grey = of(VideoFilter.Vhs, 0.5f, 0.5f, 0.5f)
        assertTrue("warm", grey[0] > grey[2])
    }

    @Test
    fun theGlMatrixIsInColumnOrderWithTheAddedValueLast() {
        val m = listOf(floatArrayOf(1f, 2f, 3f, 4f), floatArrayOf(5f, 6f, 7f, 8f), floatArrayOf(9f, 10f, 11f, 12f))
        val gl = ColourMatrix.toGl(m)
        assertEquals(2f, gl[4])
        assertEquals(4f, gl[12])
        assertEquals(8f, gl[13])
        assertEquals(1f, gl[15])
        assertEquals(0f, gl[3])
    }
}
