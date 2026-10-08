package io.github.stivengjekaj.zettastream.net

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HttpTest {
    @get:Rule val folder = TemporaryFolder()
    private val server = MockWebServer()

    @Before fun start() = server.start()
    @After fun stop() = server.close()

    private fun reply(body: String, cacheControl: String? = null) = server.enqueue(
        MockResponse.Builder().body(body).apply { cacheControl?.let { addHeader("Cache-Control", it) } }.build(),
    )

    private suspend fun read(http: okhttp3.OkHttpClient, keep: Long?) =
        Http.stream(http, server.url("/guide.xml").toString(), keep) { it.reader().readText() }

    @Test
    fun aFileWithAKeepTimeComesFromTheCacheTheSecondTime() = runTest {
        val http = Http.client(folder.root)
        reply("first")
        reply("second")
        assertEquals("first", read(http, 60))
        assertEquals("first", read(http, 60))
        assertEquals(1, server.requestCount)
        assertNull(server.takeRequest().headers[Http.KEEP_FOR])
    }

    @Test
    fun aFileWithNoKeepTimeDownloadsEachTime() = runTest {
        val http = Http.client(folder.root)
        reply("first")
        reply("second")
        assertEquals("first", read(http, null))
        assertEquals("second", read(http, null))
        assertEquals(2, server.requestCount)
    }

    @Test
    fun theTimeOfTheServerWins() = runTest {
        val http = Http.client(folder.root)
        reply("first", cacheControl = "no-store, max-age=0")
        reply("second")
        assertEquals("first", read(http, 60))
        assertEquals("second", read(http, 60))
    }
}
