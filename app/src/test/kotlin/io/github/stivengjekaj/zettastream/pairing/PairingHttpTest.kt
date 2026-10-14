package io.github.stivengjekaj.zettastream.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingHttpTest {
    @Test
    fun readsAPostWithAFormBody() {
        val body = "lines=https%3A%2F%2Fa.test%2Fmanifest.json%0D%0Ahttps%3A%2F%2Fb.test%2Fp.m3u"
        val raw = "POST /?t=abc HTTP/1.1\r\nHost: tv\r\nContent-Length: ${body.length}\r\n\r\n$body"
        val request = PairingHttp.read(raw.byteInputStream())!!
        assertEquals("POST", request.method)
        assertEquals("/", request.path)
        assertEquals("abc", request.query["t"])
        assertEquals("https://a.test/manifest.json\r\nhttps://b.test/p.m3u", PairingHttp.form(request.body)["lines"])
    }

    @Test
    fun aBadEscapeLeavesOutOnlyItsPair() {
        assertEquals(mapOf("lines" to "a b"), PairingHttp.form("t=%&lines=a+b"))
    }

    @Test
    fun refusesABodyThatIsTooLarge() {
        val raw = "POST / HTTP/1.1\r\nContent-Length: ${PairingHttp.MAX_BODY + 1}\r\n\r\n"
        assertNull(PairingHttp.read(raw.byteInputStream()) { _, _, _ -> PairingHttp.MAX_BODY })
    }

    @Test
    fun onlyAnAllowedRequestCanSendALargeBody() {
        val length = PairingHttp.SMALL_BODY + 1
        val head = "POST /backup?t=tok HTTP/1.1\r\nContent-Length: $length\r\n\r\n"
        val raw = head + "x".repeat(length)
        val allow = { _: String, path: String, query: Map<String, String> ->
            if (path == "/backup" && query["t"] == "tok") PairingHttp.MAX_BODY else PairingHttp.SMALL_BODY
        }
        assertEquals(length, PairingHttp.read(raw.byteInputStream(), allow)!!.body.length)
        assertNull(PairingHttp.read(raw.replace("t=tok", "t=bad").byteInputStream(), allow))
        assertNull(PairingHttp.read(raw.byteInputStream()))
    }

    @Test
    fun thePageEscapesTheCurrentList() {
        val page = PairingHttp.page("tok", "</textarea><script>")
        assertFalse(page.contains("</textarea><script>"))
        assertTrue(page.contains("&lt;/textarea&gt;&lt;script&gt;"))
    }

    @Test
    fun eachTokenIsNew() {
        val a = PairingServer.newToken()
        assertEquals(18, a.length)
        assertTrue(a != PairingServer.newToken())
    }
}
