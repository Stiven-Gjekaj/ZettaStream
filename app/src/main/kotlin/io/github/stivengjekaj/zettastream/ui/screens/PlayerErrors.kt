package io.github.stivengjekaj.zettastream.ui.screens

import androidx.media3.common.PlaybackException

/** Tells the viewer in plain words why a stream does not play. */
object PlayerErrors {
    /** The number of reconnects in a row, with no playback between them. */
    const val MAX_RETRIES = 3

    /**
     * A network error reconnects at the same position, because a slow server
     * often drops one request. Other errors do not change when the player tries
     * again.
     */
    fun shouldRetry(code: Int, retries: Int): Boolean = code in 2000..2999 && retries < MAX_RETRIES

    /** What the player does when a stream that played stops with an error. */
    enum class MidStream { Reconnect, SkipAhead, NextSource, Stop }

    /** Good playback this far past a fault ends the count of tries. */
    const val RECOVERED_MS = 30_000L

    /** The jump past damaged data in the middle of a video. */
    const val SKIP_MS = 5_000L

    /**
     * A stream that played and then stops: first the player reconnects at the same position.
     * Damaged data stops it again at the same place, so the second try jumps [SKIP_MS] ahead.
     * Then the next source continues at the same position. A device that cannot decode the
     * video does not change with a new try, so it goes to the next source at once.
     */
    fun midStream(code: Int, retries: Int, hasNextSource: Boolean): MidStream {
        val decoder = code in DECODER_ERRORS
        return when {
            !decoder && retries == 0 -> MidStream.Reconnect
            !decoder && retries == 1 && code in 3000..3999 -> MidStream.SkipAhead
            !decoder && retries < MAX_RETRIES && code in 2000..2999 -> MidStream.Reconnect
            hasNextSource -> MidStream.NextSource
            else -> MidStream.Stop
        }
    }

    private val DECODER_ERRORS = setOf(
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
    )

    /** The message for a stream that played and then stopped. */
    fun describeMidStream(code: Int, codeName: String): String = when (code) {
        in 3000..3999 -> "The stream sent damaged video data."
        else -> describe(code, codeName)
    }

    fun describe(code: Int, codeName: String): String = when (code) {
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
            "This link gives a web page or a file that is not a video."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
            "The server refuses this stream. The link can be old."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "The device cannot connect to the server of this stream."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
            "This device cannot decode this video. Choose a source with an other codec, for example H.264."
        else -> "This stream does not play ($codeName)."
    }
}
