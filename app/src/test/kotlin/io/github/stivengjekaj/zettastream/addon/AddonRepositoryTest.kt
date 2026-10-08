package io.github.stivengjekaj.zettastream.addon

import io.github.stivengjekaj.zettastream.source.SourceList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddonRepositoryTest {
    private val server = MockWebServer()

    @Before fun start() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.target) {
                "/good/manifest.json" -> ok("""{"id":"g","name":"Good","types":["movie"],"resources":["stream"],"catalogs":[{"type":"movie","id":"top"},{"type":"movie","id":"find","extra":[{"name":"search","isRequired":true}]}]}""")
                "/good/stream/movie/tt1.json" -> ok("""{"streams":[{"url":"https://a.test/1.mp4"},{"infoHash":"x"},{"infoHash":"y"}]}""")
                "/good/stream/movie/tt2.json" -> ok("""{"streams":[{"url":"https://a.test/1.mp4"},{"infoHash":"abcdef0123456789abcdef0123456789abcdef01","fileIdx":2,"sources":["tracker:udp://t.test:1/announce"]}]}""")
                "/subs/manifest.json" -> ok("""{"id":"s","name":"Subs","types":["movie"],"idPrefixes":["tt"],"resources":["subtitles"],"catalogs":[]}""")
                "/subs/subtitles/movie/tt1.json" -> ok("""{"subtitles":[{"id":"1","url":"https://s.test/1.srt","lang":"eng"}]}""")
                else -> MockResponse.Builder().code(404).build()
            }
        }
        server.start()
    }

    @After fun stop() = server.close()

    private fun ok(body: String) = MockResponse.Builder().body(body).build()

    @Test
    fun aBadSourceIsAFailureAndTheGoodOneIsInstalled() = runTest {
        val sources = MutableStateFlow(SourceList.parse("${server.url("/good/manifest.json")}\n${server.url("/bad/manifest.json")}"))
        val repository = AddonRepository(AddonClient(OkHttpClient()), sources, backgroundScope, fallbackSubtitlesUrl = null)
        val state = repository.addons.first { !it.loading && it.addons.isNotEmpty() }
        assertEquals(listOf("Good"), state.addons.map { it.name })
        assertEquals(1, state.failures.size)
        assertEquals(listOf("top"), repository.homeRows().map { it.catalog.id })

        val group = repository.streams("movie", "tt1").toList().single()
        assertEquals(1, group.streams.size)
        assertEquals(2, group.hidden)
    }

    @Test
    fun torrentsComeWhenTheSettingIsOn() = runTest {
        val sources = MutableStateFlow(SourceList.parse("${server.url("/good/manifest.json")}"))
        val repository = AddonRepository(AddonClient(OkHttpClient()), sources, backgroundScope, fallbackSubtitlesUrl = null)
        repository.addons.first { !it.loading && it.addons.isNotEmpty() }
        // "x" and "y" in tt1 are not 40 characters, so they are not torrents.
        assertEquals(1, repository.streams("movie", "tt1", withTorrents = true).toList().single().streams.size)
        val withTorrents = repository.streams("movie", "tt2", withTorrents = true).toList().single()
        assertEquals(2, withTorrents.streams.size)
        assertEquals(2, withTorrents.streams[1].fileIdx)
        assertEquals(1, repository.streams("movie", "tt2").toList().single().streams.size)
    }

    @Test
    fun withNoSubtitleAddonTheFallbackGivesSubtitles() = runTest {
        val sources = MutableStateFlow(SourceList.parse("${server.url("/good/manifest.json")}"))
        val repository = AddonRepository(AddonClient(OkHttpClient()), sources, backgroundScope, server.url("/subs/manifest.json").toString())
        repository.addons.first { !it.loading && it.addons.isNotEmpty() }
        assertEquals(listOf("https://s.test/1.srt"), repository.subtitles("movie", "tt1").map { it.url })
    }

    @Test
    fun withASubtitleAddonTheFallbackIsNotAsked() = runTest {
        val subs = server.url("/subs/manifest.json").toString()
        val sources = MutableStateFlow(SourceList.parse(subs))
        // The fallback is on the same server, so a request to it would add to the count.
        val repository = AddonRepository(AddonClient(OkHttpClient()), sources, backgroundScope, server.url("/fallback/manifest.json").toString())
        repository.addons.first { !it.loading && it.addons.isNotEmpty() }
        val before = server.requestCount
        assertEquals(1, repository.subtitles("movie", "tt1").size)
        assertEquals(before + 1, server.requestCount)
    }

    @Test
    fun retryInstallsTheAddonsAgain() = runTest {
        val sources = MutableStateFlow(SourceList.parse("${server.url("/good/manifest.json")}"))
        val repository = AddonRepository(AddonClient(OkHttpClient()), sources, backgroundScope, fallbackSubtitlesUrl = null)
        repository.addons.first { !it.loading && it.addons.isNotEmpty() }
        val before = server.requestCount
        repository.retry()
        repository.addons.first { it.loading }
        repository.addons.first { !it.loading }
        assertEquals(before + 1, server.requestCount)
    }
}
