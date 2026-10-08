package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayTest {
    @Test
    fun theFirstLatinAliasIsTheEnglishName() {
        assertEquals("My Hero Academia Season 2", Display.englishName("Boku no Hero Academia 2", listOf("My Hero Academia Season 2", "Boku no Hero Academia 2")))
        assertEquals("Mushoku Tensei: Jobless Reincarnation", Display.englishName("Mushoku Tensei", listOf("無職転生", "Mushoku Tensei: Jobless Reincarnation")))
    }

    @Test
    fun withNoLatinAliasTheNameStays() {
        assertEquals("Frieren", Display.englishName("Frieren", emptyList()))
        assertEquals("Frieren", Display.englishName("Frieren", listOf("葬送のフリーレン")))
    }

    @Test
    fun theImdbNumbersReplaceTheKitsuNumbers() {
        val v = Display.video(Video(id = "kitsu:12268:1", season = 1, episode = 1, imdbSeason = 2, imdbEpisode = 1))
        assertEquals(2, v.season)
        assertEquals(1, v.episode)
        assertEquals("kitsu:12268:1", v.id)
    }

    @Test
    fun withNoImdbNumbersTheVideoStays() {
        val v = Video(id = "tt1:1:3", season = 1, episode = 3)
        assertEquals(v, Display.video(v))
    }
}
