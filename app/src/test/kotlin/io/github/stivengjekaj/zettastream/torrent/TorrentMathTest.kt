package io.github.stivengjekaj.zettastream.torrent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TorrentMathTest {
    @Test
    fun theMagnetHoldsTheHashAndTheTrackersOfTheAddon() {
        val magnet = TorrentMath.magnet(
            "ABCDEF0123456789ABCDEF0123456789ABCDEF01",
            listOf("tracker:udp://a.test:80/announce", "dht:abcdef"),
            "Show S01E01",
        )
        assertTrue(magnet.startsWith("magnet:?xt=urn:btih:abcdef0123456789abcdef0123456789abcdef01&dn=Show+S01E01"))
        assertTrue(magnet.contains("&tr=udp%3A%2F%2Fa.test%3A80%2Fannounce"))
        assertFalse(magnet.contains("dht"))
        assertTrue(magnet.contains("opentrackr"))
    }

    @Test
    fun anInfoHashHasFortyHexCharacters() {
        assertTrue(TorrentMath.isInfoHash("abcdef0123456789ABCDEF0123456789abcdef01"))
        assertFalse(TorrentMath.isInfoHash("xyz"))
        assertFalse(TorrentMath.isInfoHash(null))
    }

    @Test
    fun theFileOfTheAddonComesFirstWhenItIsAVideo() {
        val names = listOf("Sample.mkv", "Show.S01E02.mkv", "Show.S01E01.mkv", "info.nfo")
        val sizes = listOf(50L, 900L, 1000L, 1L)
        assertEquals(1, TorrentMath.chooseFile(names, sizes, preferred = 1))
        assertEquals(2, TorrentMath.chooseFile(names, sizes, preferred = null))
        assertEquals(2, TorrentMath.chooseFile(names, sizes, preferred = 3))
        assertEquals(2, TorrentMath.chooseFile(names, sizes, preferred = 99))
    }

    @Test
    fun inASeasonPackTheFileNameOfTheAddonComesFirst() {
        val names = listOf("Pack/Show.S01E01.mkv", "Pack/Show.S01E02.mkv", "Pack/Show.S01E03.mkv")
        val sizes = listOf(900L, 800L, 1000L)
        assertEquals(1, TorrentMath.chooseFile(names, sizes, null, filename = "Show.S01E02.mkv"))
    }

    @Test
    fun inASeasonPackTheEpisodeNumberComesNext() {
        val names = listOf("Show.S01E01.mkv", "Show.S01E02.mkv", "Show.S01E12.mkv", "Show.S02E02.mkv")
        val sizes = listOf(900L, 800L, 1000L, 1200L)
        assertEquals(1, TorrentMath.chooseFile(names, sizes, null, season = 1, episode = 2))
        val anime = listOf("[Group] Show - 01 [1080p].mkv", "[Group] Show - 05 [1080p].mkv", "[Group] Show - 15 [1080p].mkv")
        assertEquals(1, TorrentMath.chooseFile(anime, listOf(1L, 1L, 2L), null, episode = 5))
        assertEquals(1, TorrentMath.chooseFile(listOf("a.1x01.mp4", "a.1x02.mp4"), listOf(5L, 1L), null, season = 1, episode = 2))
    }

    @Test
    fun withNoVideoTheLargestFileIsTheChoice() {
        assertEquals(0, TorrentMath.chooseFile(listOf("a.bin", "b.txt"), listOf(10L, 1L), null))
        assertEquals(-1, TorrentMath.chooseFile(emptyList(), emptyList(), null))
    }

    @Test
    fun rangesReadEachForm() {
        assertEquals(0L until 1000L, TorrentMath.range(null, 1000))
        assertEquals(100L..999L, TorrentMath.range("bytes=100-", 1000))
        assertEquals(100L..199L, TorrentMath.range("bytes=100-199", 1000))
        assertEquals(900L until 1000L, TorrentMath.range("bytes=-100", 1000))
        assertEquals(990L..999L, TorrentMath.range("bytes=990-5000", 1000))
        assertNull(TorrentMath.range("bytes=1000-", 1000))
        assertNull(TorrentMath.range("bytes=500-100", 1000))
    }

    @Test
    fun onlyTheHeadTheTailAndTheWindowAreWanted() {
        val wanted = TorrentMath.wanted(firstPiece = 10, lastPiece = 109, readPiece = 50, aheadPieces = 5, edgePieces = 2)
        assertEquals(setOf(10, 11, 108, 109, 50, 51, 52, 53, 54, 55), wanted)
    }

    @Test
    fun theWindowStopsAtTheEndOfTheFile() {
        val wanted = TorrentMath.wanted(firstPiece = 0, lastPiece = 9, readPiece = 8, aheadPieces = 5, edgePieces = 1)
        assertEquals(setOf(0, 8, 9), wanted)
        assertEquals(setOf(0, 1, 2, 3), TorrentMath.wanted(0, 3, readPiece = 0, aheadPieces = 10, edgePieces = 1))
    }

    @Test
    fun piecesForRoundsUp() {
        assertEquals(3, TorrentMath.piecesFor(2_500_000, 1_000_000))
        assertEquals(1, TorrentMath.piecesFor(0, 1_000_000))
    }

    @Test
    fun theMimeTypeComesFromTheExtension() {
        assertEquals("video/x-matroska", TorrentMath.mimeType("a.MKV"))
        assertEquals("video/mp4", TorrentMath.mimeType("a.mp4"))
    }
}
