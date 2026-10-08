package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamInfoTest {
    @Test
    fun readsTheSeedersInEachFormOfTheAddons() {
        assertEquals(304, StreamInfo.parse("Torrentio\n👤 304 💾 1.2 GB").seeders)
        assertEquals(15, StreamInfo.parse("👥 15 seeders").seeders)
        assertEquals(100, StreamInfo.parse("👤100:40").seeders)
        assertEquals(12, StreamInfo.parse("Seeders: 12").seeders)
        assertEquals(7, StreamInfo.parse("7 seeds").seeders)
        assertNull(StreamInfo.parse("1080p WEB-DL").seeders)
    }

    @Test
    fun readsTheQuality() {
        val info = StreamInfo.parse("[SubsPlease] Show - 05 (1080p) [HEVC] Dual Audio\n💾 1.45 GB")
        assertEquals(1080, info.resolution)
        assertEquals("HEVC", info.codec)
        assertTrue(info.dualAudio)
        assertEquals(1_450_000_000L, info.sizeBytes)
        assertEquals("SubsPlease", info.group)
        assertEquals(2160, StreamInfo.parse("Movie 4K HDR10 Remux").resolution)
        assertEquals("HDR", StreamInfo.parse("Movie 2160p HDR x265").hdr)
        assertEquals("Dolby Vision", StreamInfo.parse("Movie 2160p DV").hdr)
        assertEquals("WEB-DL", StreamInfo.parse("Show.S01E01.720p.WEB-DL.x264").source)
        assertFalse(StreamInfo.parse("Show 720p").dualAudio)
        // A file that ends in .ts is a transport stream, not a camera copy.
        assertNull(StreamInfo.parse("Episode 5.ts").source)
    }

    @Test
    fun theBadgesAreShortAndInOrder() {
        val info = StreamInfo.parse("Movie 2160p HDR x265 BluRay 12.3 GB 👤 50")
        assertEquals(listOf("4K", "HDR", "HEVC", "BluRay", "12.3 GB", "50 seeders"), info.badges)
    }

    @Test
    fun aSourceWithTheSameDataScoresHigher() {
        val current = StreamInfo.parse("[SubsPlease] Show - 05 (1080p) HEVC")
        val same = StreamInfo.parse("[SubsPlease] Show - 06 (1080p) HEVC")
        val other = StreamInfo.parse("[Other] Show - 06 (720p) x264 Dual Audio")
        assertTrue(StreamInfo.similarity(current, same) > StreamInfo.similarity(current, other))
    }
}
