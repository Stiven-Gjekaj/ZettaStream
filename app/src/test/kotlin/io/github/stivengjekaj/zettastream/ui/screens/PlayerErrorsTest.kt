package io.github.stivengjekaj.zettastream.ui.screens

import androidx.media3.common.PlaybackException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private val network = PlaybackException.ERROR_CODE_IO_UNSPECIFIED

    @Test
    fun aNetworkErrorReconnectsUpToTheLimit() {
        assertTrue(PlayerErrors.shouldRetry(network, retries = 0))
        assertFalse(PlayerErrors.shouldRetry(network, retries = PlayerErrors.MAX_RETRIES))
    }

    @Test
    fun aFormatErrorDoesNotReconnect() {
        assertFalse(PlayerErrors.shouldRetry(PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED, 0))
    }

    @Test
    fun damagedDataReconnectsThenSkipsThenGoesToTheNextSource() {
        val damaged = PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED
        assertEquals(PlayerErrors.MidStream.Reconnect, PlayerErrors.midStream(damaged, 0, hasNextSource = true))
        assertEquals(PlayerErrors.MidStream.SkipAhead, PlayerErrors.midStream(damaged, 1, hasNextSource = true))
        assertEquals(PlayerErrors.MidStream.NextSource, PlayerErrors.midStream(damaged, 2, hasNextSource = true))
        assertEquals(PlayerErrors.MidStream.Stop, PlayerErrors.midStream(damaged, 2, hasNextSource = false))
    }

    @Test
    fun aNetworkErrorReconnectsThreeTimesAndADecoderErrorChangesTheSource() {
        val network = PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        assertEquals(PlayerErrors.MidStream.Reconnect, PlayerErrors.midStream(network, 2, hasNextSource = true))
        assertEquals(PlayerErrors.MidStream.NextSource, PlayerErrors.midStream(network, 3, hasNextSource = true))
        val decoder = PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
        assertEquals(PlayerErrors.MidStream.NextSource, PlayerErrors.midStream(decoder, 0, hasNextSource = true))
        assertEquals(PlayerErrors.MidStream.Stop, PlayerErrors.midStream(decoder, 0, hasNextSource = false))
    }
}
