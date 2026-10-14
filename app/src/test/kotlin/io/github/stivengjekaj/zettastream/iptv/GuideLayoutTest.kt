package io.github.stivengjekaj.zettastream.iptv

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideLayoutTest {
    private val hour = 60 * 60_000L
    private fun p(title: String, start: Long, stop: Long) = Programme("ch", title, start, stop)

    @Test
    fun theWindowStartsAtTheLastHalfHour() {
        assertEquals(10 * hour + 30 * 60_000L, GuideLayout.windowStart(10 * hour + 47 * 60_000L))
    }

    @Test
    fun programmesAreCutAtTheEdgesOfTheWindow() {
        val from = 10 * hour
        val blocks = GuideLayout.blocks(
            listOf(p("late", 14 * hour, 15 * hour), p("news", 9 * hour, 11 * hour), p("film", 11 * hour, 14 * hour), p("old", 8 * hour, 9 * hour)),
            from,
        )
        assertEquals(listOf("news", "film"), blocks.map { it.programme.title })
        assertEquals(0f, blocks[0].offset, 0.0001f)
        assertEquals(1f / 3, blocks[0].width, 0.0001f)
        assertEquals(1f / 3, blocks[1].offset, 0.0001f)
        assertEquals(2f / 3, blocks[1].width, 0.0001f)
    }
}
