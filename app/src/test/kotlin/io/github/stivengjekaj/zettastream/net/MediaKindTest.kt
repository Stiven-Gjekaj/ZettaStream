package io.github.stivengjekaj.zettastream.net

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MediaKindTest {
    private val server = MockWebServer()

    @Before fun start() = server.start()
    @After fun stop() = server.close()

    @Test
    fun anHlsLinkInsideAProxyParameterIsHls() {
        assertEquals(MediaKind.Hls, MediaKinds.fromUrl("http://1.2.3.4/proxy/bound?d=https%3A%2F%2Fcdn.test%2Fhls2%2Findex.m3u8%3Ft%3D1"))
    }

    @Test
    fun theEndingOfThePathDecides() {
        assertEquals(MediaKind.Hls, MediaKinds.fromUrl("https://cdn.test/a/master.m3u8?token=1"))
        assertEquals(MediaKind.File, MediaKinds.fromUrl("http://127.0.0.1:4000/abc/Big%20Buck%20Bunny.mp4"))
        assertNull(MediaKinds.fromUrl("https://cdn.test/a/,l,n,h,.urlset/master.txt"))
    }

    @Test
    fun theAnswerDecidesWhenTheLinkDoesNotTell() {
        assertEquals(MediaKind.Hls, MediaKinds.fromAnswer("application/vnd.apple.mpegurl", ByteArray(0)))
        assertEquals(MediaKind.Hls, MediaKinds.fromAnswer("text/plain", "#EXTM3U\n#EXT-X-VERSION:3".toByteArray()))
        assertEquals(MediaKind.File, MediaKinds.fromAnswer("video/mp4", ByteArray(4)))
        assertNull(MediaKinds.fromAnswer("text/html", "<html>".toByteArray()))
    }

    @Test
    fun detectAsksTheServerForALinkThatDoesNotTell() = runTest {
        server.enqueue(MockResponse.Builder().addHeader("Content-Type", "text/plain").body("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1").build())
        val kind = MediaKinds.detect(OkHttpClient(), server.url("/a/master.txt").toString(), mapOf("Referer" to "https://site.test/"))
        assertEquals(MediaKind.Hls, kind)
        val request = server.takeRequest()
        assertEquals("https://site.test/", request.headers["Referer"])
        assertEquals("bytes=0-1023", request.headers["Range"])
    }
}
