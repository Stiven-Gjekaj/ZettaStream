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

    @Test
    fun anAnimeEpisodeHasAnImdbFormForSubtitles() {
        val meta = MetaPreview(id = "kitsu:7442", type = "anime", imdbId = "tt2560140")
        val episodes = listOf(Video(id = "kitsu:7442:1", season = 1, episode = 1, imdbSeason = 1, imdbEpisode = 1))
        assertEquals("series" to "tt2560140:1:1", Display.imdbAlias(meta, "kitsu:7442:1", episodes))
        assertEquals("movie" to "tt2560140", Display.imdbAlias(meta.copy(type = "movie"), "kitsu:7442", emptyList()))
        assertEquals(null, Display.imdbAlias(meta.copy(id = "tt2560140"), "tt2560140:1:1", episodes))
        assertEquals(null, Display.imdbAlias(meta.copy(imdbId = null), "kitsu:7442:1", episodes))
    }

    @Test
    fun aCardShowsTheKindAndTheFirstYear() {
        assertEquals("Movie · 2026", Display.cardFacts("movie", "2026"))
        assertEquals("Series · 2019", Display.cardFacts("series", "2019–2023"))
        assertEquals("Anime · 2024", Display.cardFacts("anime", "Oct 2024"))
        assertEquals("Channel", Display.cardFacts("tv", null))
        assertEquals("Series", Display.cardFacts("series", "soon"))
    }

    @Test
    fun aRatingHasOneDecimalAndOnlyAValidNumberShows() {
        assertEquals("7.4", Display.rating("7.4"))
        assertEquals("8.0", Display.rating(" 8 "))
        assertEquals(null, Display.rating("0"))
        assertEquals(null, Display.rating("N/A"))
        assertEquals(null, Display.rating("74"))
        assertEquals(null, Display.rating(null))
    }
}
