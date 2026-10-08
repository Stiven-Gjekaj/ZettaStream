package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.ui.screens.seasonSuffix
import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonSuffixTest {
    private fun base(name: String) = name.replace(seasonSuffix, "")

    @Test
    fun removesTheSeasonFromTheEndOfAName() {
        assertEquals("My Hero Academia", base("My Hero Academia Season 2"))
        assertEquals("Re:Zero kara Hajimeru Isekai Seikatsu", base("Re:Zero kara Hajimeru Isekai Seikatsu 4th Season"))
        assertEquals("Spy x Family", base("Spy x Family Part 2"))
        assertEquals("Attack on Titan", base("Attack on Titan"))
        assertEquals("Mob Psycho 100", base("Mob Psycho 100"))
    }
}
