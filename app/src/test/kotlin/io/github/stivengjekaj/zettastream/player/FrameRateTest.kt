package io.github.stivengjekaj.zettastream.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FrameRateTest {
    private val m60 = DisplayMode(1, 1920, 1080, 60f)
    private val m50 = DisplayMode(2, 1920, 1080, 50f)
    private val m24 = DisplayMode(3, 1920, 1080, 24f)
    private val m2398 = DisplayMode(4, 1920, 1080, 23.976f)
    private val uhd24 = DisplayMode(5, 3840, 2160, 24f)
    private val modes = listOf(m60, m50, m24, m2398, uhd24)

    @Test
    fun aFilmGoesTo24HzAtTheSameSize() {
        assertEquals(m24, FrameRate.choose(modes, m60, 24f))
        assertEquals(m2398, FrameRate.choose(modes, m60, 23.976f))
    }

    @Test
    fun aEuropeanBroadcastGoesTo50Hz() {
        assertEquals(m50, FrameRate.choose(modes, m60, 25f))
    }

    @Test
    fun aModeThatFitsAlreadyStays() {
        assertNull(FrameRate.choose(modes, m60, 30f))
        assertNull(FrameRate.choose(modes, m60, 60f))
    }

    @Test
    fun noFittingModeChangesNothing() {
        assertNull(FrameRate.choose(listOf(m60), m60, 24f))
        assertNull(FrameRate.choose(modes, m60, 0f))
    }

    @Test
    fun theRateComesFromTheTimesOfTheFrames() {
        val film = (0 until 48).map { it * 41_708L } // 23.976 fps, rounded to whole microseconds
        assertEquals(23.976f, FrameRate.nearestCommon(FrameRate.fromTimes(film)!!), 0.0001f)
        val pal = (0 until 48).map { it * 40_000L } - 400_000L // one frame missing
        assertEquals(25f, FrameRate.nearestCommon(FrameRate.fromTimes(pal)!!), 0.0001f)
        // Real times of a 60 fps stream, rounded to the microsecond.
        val rounded = (0 until 48).map { (it * 1_000_000.0 / 60).toLong() + if (it % 3 == 0) 12 else 0 }
        assertEquals(60f, FrameRate.nearestCommon(FrameRate.fromTimes(rounded)!!), 0.0001f)
        assertNull(FrameRate.fromTimes(listOf(0L, 40_000L)))
    }
}

