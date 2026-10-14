package io.github.stivengjekaj.zettastream.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHistoryTest {
    @Test
    fun aNewSearchGoesToTheTopAndARepeatMoves() {
        var h = SearchHistory.add(emptyList(), " frieren ")
        h = SearchHistory.add(h, "dragon")
        h = SearchHistory.add(h, "Frieren")
        assertEquals(listOf("Frieren", "dragon"), h)
        assertEquals(h, SearchHistory.add(h, "   "))
    }

    @Test
    fun theHistoryKeepsOnlyTheNewest() {
        val h = (1..12).fold(emptyList<String>()) { acc, i -> SearchHistory.add(acc, "q$i") }
        assertEquals(SearchHistory.MAX, h.size)
        assertEquals("q12", h.first())
        assertEquals("q5", h.last())
    }
}
