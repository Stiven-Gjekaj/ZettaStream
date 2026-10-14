package io.github.stivengjekaj.zettastream.library

import io.github.stivengjekaj.zettastream.addon.Meta
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.addon.Video
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class NewEpisodesTest {
    private val now = Instant.parse("2026-10-14T12:00:00Z").toEpochMilli()

    private fun show(id: String, vararg episodes: Pair<String, String?>) =
        Meta(id = id, type = "series", name = id, videos = episodes.map { (vid, date) -> Video(vid, released = date) })

    @Test
    fun eachShowGivesItsNewestEpisodeOfTheWeek() {
        val a = show("a", "a:1:1" to "2026-10-01T00:00:00Z", "a:1:2" to "2026-10-09T00:00:00Z", "a:1:3" to "2026-10-13T00:00:00Z")
        val b = show("b", "b:1:5" to "2026-10-12T00:00:00Z", "b:1:6" to "2026-10-20T00:00:00Z")
        val c = show("c", "c:1:1" to "2026-09-01T00:00:00Z", "c:1:2" to null)
        val found = NewEpisodes.find(listOf(a, b, c), now, watched = emptySet())
        assertEquals(listOf("a:1:3", "b:1:5"), found.map { it.video.id })
    }

    @Test
    fun aWatchedEpisodeIsNotNew() {
        val a = show("a", "a:1:2" to "2026-10-09T00:00:00Z", "a:1:3" to "2026-10-13T00:00:00Z")
        assertEquals(listOf("a:1:2"), NewEpisodes.find(listOf(a), now, watched = setOf("a:1:3")).map { it.video.id })
    }

    @Test
    fun theFollowedShowsAreTheWatchlistAndTheHistoryWithNoFilms() {
        val data = LibraryData(
            watchlist = listOf(MetaPreview("s1", "series", "One"), MetaPreview("m1", "movie", "Film")),
            progress = mapOf(
                "s2:1:1" to Progress("s2:1:1", MetaPreview("s2", "anime", "Two"), "E1", 10, 100, updated = 2),
                "s1:1:1" to Progress("s1:1:1", MetaPreview("s1", "series", "One"), "E1", 10, 100, updated = 1),
            ),
        )
        assertEquals(listOf("s1", "s2"), NewEpisodes.followed(data).map { it.id })
    }

    @Test
    fun aShowWithOneDateForEachEpisodeIsLeftOut() {
        val fake = show("f", "f:1:1" to "2026-10-14T10:00:00.000Z", "f:1:2" to "2026-10-14T10:00:00.000Z")
        val single = show("s", "s:1:1" to "2026-10-14T10:00:00.000Z")
        assertEquals(listOf("s:1:1"), NewEpisodes.find(listOf(fake, single), now, watched = emptySet()).map { it.video.id })
    }
}
