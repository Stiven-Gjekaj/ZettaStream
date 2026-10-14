package io.github.stivengjekaj.zettastream.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeTest {
    @Test
    fun aPositionInsideTheVideoFits() {
        assertTrue(Resume.fits(position = 600_000, duration = 5_796_000))
    }

    @Test
    fun aPositionPastTheEndOfAShorterSourceDoesNotFit() {
        assertFalse(Resume.fits(position = 982_890, duration = 634_584))
        assertFalse(Resume.fits(position = 630_000, duration = 634_584))
    }
}
