package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.ui.screens.count
import org.junit.Assert.assertEquals
import org.junit.Test

class CountTest {
    @Test
    fun oneHasNoPluralEnding() {
        assertEquals("1 stream", count(1, "stream"))
        assertEquals("0 streams", count(0, "stream"))
        assertEquals("3 torrent streams", count(3, "torrent stream"))
    }
}
