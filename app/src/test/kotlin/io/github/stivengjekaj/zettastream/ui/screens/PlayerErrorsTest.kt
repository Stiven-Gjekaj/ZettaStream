package io.github.stivengjekaj.zettastream.ui.screens

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerErrorsTest {
    @Test
    fun aWebPageGetsItsOwnMessage() {
        val text = PlayerErrors.describe(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, "ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED")
        assertTrue(text, "web page" in text)
    }

    @Test
    fun anUnknownCodeShowsItsName() {
        assertEquals("This stream does not play (ERROR_CODE_X).", PlayerErrors.describe(-12345, "ERROR_CODE_X"))
    }
}
