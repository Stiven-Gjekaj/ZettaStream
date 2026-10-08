package io.github.stivengjekaj.zettastream.iptv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class XmltvTest {
    private fun ms(text: String) = Instant.parse(text).toEpochMilli()

    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <tv>
          <channel id="news.test"><display-name>News</display-name></channel>
          <programme start="20261008100000 +0000" stop="20261008110000 +0000" channel="news.test">
            <title lang="en">Morning</title><desc>The news.</desc>
          </programme>
          <programme start="20261008130000 +0200" stop="20261008120000 +0000" channel="news.test">
            <title>Noon</title>
          </programme>
          <programme start="20261008100000 +0000" stop="20261008110000 +0000" channel="other.test">
            <title>Other</title>
          </programme>
          <programme start="20261001100000 +0000" stop="20261001110000 +0000" channel="news.test">
            <title>Old</title>
          </programme>
        </tv>
    """.trimIndent()

    private fun guide() = Xmltv.parse(
        xml.byteInputStream(), setOf("news.test"),
        from = ms("2026-10-08T00:00:00Z"), to = ms("2026-10-09T00:00:00Z"),
    )

    @Test
    fun keepsOnlyTheChosenChannelsAndTheTimeWindow() {
        val guide = guide()
        assertEquals(listOf("Morning", "Noon"), guide.programmes("news.test").map { it.title })
        assertEquals(emptyList<Programme>(), guide.programmes("other.test"))
        assertEquals("The news.", guide.programmes("news.test")[0].description)
    }

    @Test
    fun readsTheTimeZoneOffset() {
        assertEquals(ms("2026-10-08T11:00:00Z"), guide().programmes("news.test")[1].start)
        assertEquals(ms("2026-10-08T12:00:00Z"), Xmltv.parseTime("20261008120000"))
        assertEquals(0L, Xmltv.parseTime("bad"))
    }

    @Test
    fun nowAndNextFindsTheCurrentAndTheNextProgramme() {
        val guide = guide()
        val (now, next) = guide.nowAndNext("news.test", ms("2026-10-08T10:30:00Z"))
        assertEquals("Morning", now?.title)
        assertEquals("Noon", next?.title)
        val (earlyNow, earlyNext) = guide.nowAndNext("news.test", ms("2026-10-08T09:00:00Z"))
        assertNull(earlyNow)
        assertEquals("Morning", earlyNext?.title)
    }
}
