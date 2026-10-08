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
}
