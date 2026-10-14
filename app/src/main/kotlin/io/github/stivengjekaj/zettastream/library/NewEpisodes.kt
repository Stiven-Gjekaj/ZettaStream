package io.github.stivengjekaj.zettastream.library

import io.github.stivengjekaj.zettastream.addon.Meta
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.addon.Video
import java.time.Instant

/** The newest episode of a show that came out a short time ago. */
data class NewEpisode(val meta: Meta, val video: Video, val released: Long)

/** Finds the episodes of the shows that the viewer follows that came out this week. */
object NewEpisodes {
    const val WINDOW_MS = 7L * 24 * 60 * 60 * 1000

    /** The most shows that the row asks the addons about. */
    const val MAX_SHOWS = 20

    /** The shows that the viewer follows: the watchlist and the shows in the history, newest first. */
    fun followed(data: LibraryData): List<MetaPreview> {
        val history = data.progress.values.sortedByDescending { it.updated }.map { it.meta }
        return (data.watchlist + history).filter { it.type.lowercase() != "movie" }.distinctBy { it.id }.take(MAX_SHOWS)
    }

    /**
     * For each show, the newest episode that came out in the last week and that the viewer
     * did not watch. The newest come first.
     */
    fun find(metas: List<Meta>, now: Long, watched: Set<String>): List<NewEpisode> = metas.mapNotNull { meta ->
        // Some addons give each episode the same date, often the day of the answer. Those dates say nothing.
        val dates = meta.videos.mapNotNull { time(it.released) }
        if (dates.size > 1 && dates.distinct().size == 1) return@mapNotNull null
        meta.videos.mapNotNull { video ->
            val released = time(video.released) ?: return@mapNotNull null
            if (released > now || released < now - WINDOW_MS || video.id in watched) null else NewEpisode(meta, video, released)
        }.maxByOrNull { it.released }
    }.sortedByDescending { it.released }

    private fun time(text: String?): Long? = text?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
}
