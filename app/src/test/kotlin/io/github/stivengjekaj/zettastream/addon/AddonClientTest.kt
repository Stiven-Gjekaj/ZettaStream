package io.github.stivengjekaj.zettastream.addon

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddonClientTest {
    private val server = MockWebServer()
    private val client = AddonClient(OkHttpClient())

    @Before fun start() = server.start()
    @After fun stop() = server.close()

    private fun reply(body: String) = server.enqueue(MockResponse.Builder().body(body).build())

    private suspend fun installed(): Addon {
        reply("""{"id":"t","name":"Test","types":["series"],"resources":["catalog","meta","stream"],"catalogs":[{"type":"series","id":"pop"}]}""")
        return client.install(server.url("/manifest.json").toString())
    }

    @Test
    fun installReadsTheManifest() = runTest {
        val addon = installed()
        assertEquals("Test", addon.name)
        assertEquals("/manifest.json", server.takeRequest().target)
    }

    @Test
    fun catalogReadsTheMetasAndIgnoresUnknownFields() = runTest {
        val addon = installed()
        reply("""{"metas":[{"id":"tt1","type":"series","name":"One","releaseInfo":2020,"unknown":true},{"id":"","type":"series"}]}""")
        val metas = client.catalog(addon, addon.manifest.catalogs.first())
        assertEquals(1, metas.size)
        assertEquals("2020", metas[0].releaseInfo)
        server.takeRequest()
        assertEquals("/catalog/series/pop.json", server.takeRequest().target)
    }

    @Test
    fun metaReadsTheEpisodes() = runTest {
        val addon = installed()
        reply("""{"meta":{"id":"tt1","type":"series","name":"One","videos":[{"id":"tt1:1:1","title":"Pilot","season":1,"episode":1}]}}""")
        val meta = client.meta(addon, "series", "tt1")!!
        assertEquals("Pilot", meta.videos.single().label)
    }

    @Test
    fun streamsKeepTheHeadersAndMarkTorrentsAsNotPlayable() = runTest {
        val addon = installed()
        reply(
            """
            {"streams":[
              {"url":"https://cdn.test/a.m3u8","name":"1080p","behaviorHints":{"proxyHeaders":{"request":{"Referer":"https://site.test/"}}}},
              {"infoHash":"abc","name":"Torrent"},
              {"ytId":"xyz"}
            ]}
            """,
        )
        val streams = client.streams(addon, "series", "tt1:1:1")
        assertTrue(streams[0].isPlayable)
        assertEquals(mapOf("Referer" to "https://site.test/"), streams[0].requestHeaders)
        assertFalse(streams[1].isPlayable)
        assertFalse(streams[2].isPlayable)
    }
}
