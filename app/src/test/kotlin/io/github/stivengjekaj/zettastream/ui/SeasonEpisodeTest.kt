package io.github.stivengjekaj.zettastream.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SeasonEpisodeTest {
    @Test
    fun readsTheSeasonAndTheEpisodeFromAVideoId() {
        assertEquals(1 to 2, seasonEpisode("tt0903747:1:2"))
        assertEquals(null to 5, seasonEpisode("kitsu:1376:5"))
        assertEquals(null to null, seasonEpisode("tt0111161"))
    }
}
