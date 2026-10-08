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
        val repository = AddonRepository(AddonClient(OkHttpClient()), sources, backgroundScope)
        val state = repository.addons.first { !it.loading && it.addons.isNotEmpty() }
        assertEquals(listOf("Good"), state.addons.map { it.name })
        assertEquals(1, state.failures.size)
        assertEquals(listOf("top"), repository.homeRows().map { it.catalog.id })

        val group = repository.streams("movie", "tt1").toList().single()
        assertEquals(1, group.streams.size)
        assertEquals(2, group.hidden)
    }
}
