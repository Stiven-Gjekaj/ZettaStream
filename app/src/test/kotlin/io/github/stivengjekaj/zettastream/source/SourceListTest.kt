package io.github.stivengjekaj.zettastream.source

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceListTest {
    @Test
    fun eachLineBecomesOneSourceOfTheCorrectKind() {
        val sources = SourceList.parse(
            """
              https://a.test/config/manifest.json
            stremio://b.test/manifest.json
            https://c.test/playlist.m3u
            https://c.test/list?type=m3u_plus
            https://d.test/guide.xml.gz
            https://d.test/epg/guide
            """.trimIndent(),
        )
        assertEquals(
            listOf(
                Source("https://a.test/config/manifest.json", SourceKind.Addon),
                Source("https://b.test/manifest.json", SourceKind.Addon),
                Source("https://c.test/playlist.m3u", SourceKind.Playlist),
                Source("https://c.test/list?type=m3u_plus", SourceKind.Playlist),
                Source("https://d.test/guide.xml.gz", SourceKind.Guide),
                Source("https://d.test/epg/guide", SourceKind.Guide),
            ),
            sources,
        )
    }

    @Test
    fun blankLinesCommentsTextAndDuplicatesAreIgnored() {
        val sources = SourceList.parse("\n# my list\nnot a url\r\nhttps://a.test/manifest.json\r\nhttps://a.test/manifest.json\n")
        assertEquals(listOf(Source("https://a.test/manifest.json", SourceKind.Addon)), sources)
    }

    @Test
    fun formatWritesOneUrlOnEachLine() {
        val text = "https://a.test/manifest.json\nhttps://c.test/p.m3u"
        assertEquals(text, SourceList.format(SourceList.parse(text)))
    }
}
