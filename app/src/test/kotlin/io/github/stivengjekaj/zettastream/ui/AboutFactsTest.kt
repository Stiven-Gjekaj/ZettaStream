package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.addon.Meta
import io.github.stivengjekaj.zettastream.addon.Video
import io.github.stivengjekaj.zettastream.ui.screens.aboutFacts
import org.junit.Assert.assertEquals
import org.junit.Test

class AboutFactsTest {
    @Test
    fun aSeriesShowsItsSeasonsAndEpisodes() {
        val meta = Meta(
            id = "tt1", type = "series", name = "Show", releaseInfo = "2019-2023", imdbRating = "8.4",
            genres = listOf("Drama", "Crime"),
            videos = listOf(Video("a", season = 0, episode = 1), Video("b", season = 1, episode = 1), Video("c", season = 2, episode = 1)),
        )
        assertEquals(
            listOf(
                "Type" to "Series", "Released" to "2019-2023", "IMDb" to "8.4 / 10", "Genres" to "Drama, Crime",
                "Seasons" to "2", "Episodes" to "3",
            ),
            aboutFacts(meta),
        )
    }

    @Test
    fun aFilmShowsNoEpisodesAndAMissingFactDoesNotShow() {
        val meta = Meta(id = "tt2", type = "movie", name = "Film", runtime = "118 min", aliases = listOf("Film", "Le Film"), videos = listOf(Video("tt2")))
        assertEquals(listOf("Type" to "Movie", "Runtime" to "118 min", "Other names" to "Le Film"), aboutFacts(meta))
    }
}
