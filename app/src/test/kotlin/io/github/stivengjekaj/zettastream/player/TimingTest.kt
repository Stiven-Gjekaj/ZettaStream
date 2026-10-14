package io.github.stivengjekaj.zettastream.player

import org.junit.Assert.assertEquals
import org.junit.Test

class TimingTest {
    @Test
    fun aStepStaysInsideTheLimit() {
        assertEquals(250L, Timing.step(0, 1, Timing.SUBTITLE_STEP, Timing.SUBTITLE_LIMIT))
        assertEquals(-250L, Timing.step(0, -1, Timing.SUBTITLE_STEP, Timing.SUBTITLE_LIMIT))
        assertEquals(2_000L, Timing.step(1_980, 1, Timing.AUDIO_STEP, Timing.AUDIO_LIMIT))
        assertEquals(-2_000L, Timing.step(-2_000, -1, Timing.AUDIO_STEP, Timing.AUDIO_LIMIT))
    }

    @Test
    fun theLabelShowsTheSignAndTheSeconds() {
        assertEquals("None", Timing.label(0))
        assertEquals("+0.25 s", Timing.label(250))
        assertEquals("-1.5 s", Timing.label(-1_500))
        assertEquals("+2 s", Timing.label(2_000))
        assertEquals("+0.05 s", Timing.label(50))
    }
}
