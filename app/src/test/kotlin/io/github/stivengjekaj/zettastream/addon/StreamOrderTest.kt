package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Test

class StreamOrderTest {
    private fun addon(name: String) = Addon("https://$name.test/manifest.json", Manifest(id = name, name = name))
    private fun torrent(seeds: Int) = Stream(infoHash = "a".repeat(39) + seeds % 10, name = "T 👤 $seeds")
    private fun direct(name: String) = Stream(url = "https://cdn.test/$name.m3u8", name = name)

    private val groups = listOf(
        StreamGroup(addon("A"), listOf(torrent(5), direct("a1"), torrent(50))),
        StreamGroup(addon("B"), listOf(torrent(20), direct("b1"))),
        StreamGroup(addon("C"), emptyList()),
    )

    @Test
    fun eachAddonKeepsItsPartWithTorrentsSortedBySeeders() {
        val sections = StreamOrder.sections(groups, directFirst = false)
        assertEquals(listOf("A", "B"), sections.map { it.title })
        assertEquals(listOf("a1", "T 👤 50", "T 👤 5"), sections[0].streams.map { it.stream.name })
    }

    @Test
    fun directLinksFirstPutsAllTorrentsLastBySeeders() {
        val sections = StreamOrder.sections(groups, directFirst = true)
        assertEquals(listOf("Direct links", "Torrents"), sections.map { it.title })
        assertEquals(listOf("a1", "b1"), sections[0].streams.map { it.stream.name })
        assertEquals(listOf(50, 20, 5), sections[1].streams.map { it.info.seeders })
    }

    private fun named(name: String, torrent: Boolean, seeds: Int = 50) =
        if (torrent) Stream(infoHash = "b".repeat(40), name = "$name 👤 $seeds") else Stream(url = "https://cdn.test/${name.length}.mp4", name = name)

    private fun bestOf(preferDirect: Boolean, vararg streams: Stream) =
        StreamOrder.best(listOf(StreamGroup(addon("A"), streams.toList())), preferDirect)?.stream?.name?.substringBefore(" 👤")

    @Test
    fun aStreamAt1080pWinsOverHigherAndLower() {
        assertEquals("1080p", bestOf(true, named("2160p", false), named("720p", false), named("1080p", false)))
    }

    @Test
    fun withNo1080pTheNearestLowerResolutionWins() {
        assertEquals("720p", bestOf(true, named("2160p", false), named("480p", false), named("720p", false)))
    }

    @Test
    fun theSettingChoosesTheKindAtTheSameResolution() {
        assertEquals("1080p web", bestOf(true, named("1080p bt", true), named("1080p web", false)))
        assertEquals("1080p bt", bestOf(false, named("1080p web", false), named("1080p bt", true)))
    }

    @Test
    fun resolutionComesBeforeTheKind() {
        assertEquals("1080p bt", bestOf(true, named("720p web", false), named("1080p bt", true)))
    }

    @Test
    fun aTorrentWithAlmostNoSeedersComesAfterADirectLink() {
        assertEquals("1080p web", bestOf(false, named("1080p bt", true, seeds = 1), named("1080p web", false)))
    }

    @Test
    fun moreSeedersWinAmongTorrents() {
        assertEquals("1080p b", bestOf(false, named("1080p a", true, seeds = 10), named("1080p b", true, seeds = 90)))
    }

    @Test
    fun noStreamGivesNothing() {
        assertEquals(null, StreamOrder.best(emptyList(), preferDirect = true))
    }
}

