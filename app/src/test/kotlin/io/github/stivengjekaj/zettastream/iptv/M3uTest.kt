package io.github.stivengjekaj.zettastream.iptv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uTest {
    @Test
    fun readsTheChannelAttributesAndTheGuideUrls() {
        val playlist = M3u.parse(
            """
            #EXTM3U x-tvg-url="https://g.test/a.xml, https://g.test/b.xml.gz"
            #EXTINF:-1 tvg-id="news.test" tvg-logo="https://l.test/n.png" group-title="News",News One
            https://s.test/news.m3u8
            #EXTINF:-1,Plain
            http://s.test/plain.ts
            """.trimIndent(),
        )
        assertEquals(listOf("https://g.test/a.xml", "https://g.test/b.xml.gz"), playlist.guideUrls)
        val (news, plain) = playlist.channels
        assertEquals("News One", news.name)
        assertEquals("news.test", news.tvgId)
        assertEquals("News", news.group)
        assertEquals("https://l.test/n.png", news.logo)
        assertEquals("http://s.test/plain.ts", plain.url)
        assertNull(plain.group)
        assertEquals("http://s.test/plain.ts", plain.key)
    }

    @Test
    fun readsHeadersFromVlcOptionsJsonAndThePipeForm() {
        val playlist = M3u.parse(
            """
            #EXTM3U
            #EXTINF:-1,One
            #EXTVLCOPT:http-user-agent=Agent/1
            #EXTVLCOPT:http-referrer=https://r.test/
            https://s.test/1.m3u8
            #EXTINF:-1,Two
            #EXTHTTP:{"Origin":"https://o.test"}
            https://s.test/2.m3u8|User-Agent=Agent%2F2&Referer=https://r2.test/
            """.trimIndent(),
        )
        assertEquals(mapOf("User-Agent" to "Agent/1", "Referer" to "https://r.test/"), playlist.channels[0].headers)
        assertEquals("https://s.test/2.m3u8", playlist.channels[1].url)
        assertEquals(
            mapOf("Origin" to "https://o.test", "User-Agent" to "Agent/2", "Referer" to "https://r2.test/"),
            playlist.channels[1].headers,
        )
    }

    @Test
    fun theHeadersOfOneChannelDoNotGoToTheNext() {
        val playlist = M3u.parse("#EXTM3U\n#EXTINF:-1,A\n#EXTVLCOPT:http-user-agent=X\nhttps://a\n#EXTINF:-1,B\nhttps://b")
        assertEquals(emptyMap<String, String>(), playlist.channels[1].headers)
    }

    @Test
    fun aCommaInTheAttributesDoesNotBreakTheName() {
        val playlist = M3u.parse("#EXTM3U\n#EXTINF:-1 group-title=\"A, B\",Name, with comma\nhttps://a")
        assertEquals("A, B", playlist.channels[0].group)
        assertEquals("Name, with comma", playlist.channels[0].name)
    }
}
