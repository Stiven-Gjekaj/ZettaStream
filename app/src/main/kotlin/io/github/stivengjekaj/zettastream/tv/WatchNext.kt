package io.github.stivengjekaj.zettastream.tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.tvprovider.media.tv.TvContractCompat
import androidx.tvprovider.media.tv.WatchNextProgram
import io.github.stivengjekaj.zettastream.diag.Diagnostics
import io.github.stivengjekaj.zettastream.library.Progress

/**
 * Puts the titles that the viewer did not finish into the "Play next" row of
 * the home screen of the TV. The viewer can remove one there. The app then
 * does not add it again until the viewer plays it again.
 *
 * Lint marks the builder methods as restricted by mistake. They are public in
 * WatchNextProgram.Builder, and they come from a base class that is restricted.
 */
@SuppressLint("RestrictedApi")
class WatchNext(private val context: Context) {
    val available: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

    fun sync(items: List<Progress>) {
        if (!available) return
        runCatching {
            val resolver = context.contentResolver
            val existing = mutableMapOf<String, WatchNextProgram>()
            resolver.query(TvContractCompat.WatchNextPrograms.CONTENT_URI, WatchNextProgram.PROJECTION, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val program = WatchNextProgram.fromCursor(cursor)
                    program.internalProviderId?.let { existing[it] = program }
                }
            }
            val wanted = items.take(MAX_ITEMS).associateBy { it.meta.id }
            for ((id, progress) in wanted) {
                val old = existing[id]
                // The viewer removed it from the row, and did not play it after that.
                if (old != null && !old.isBrowsable && old.lastEngagementTimeUtcMillis >= progress.updated) continue
                val values = program(progress).toContentValues()
                if (old == null) resolver.insert(TvContractCompat.WatchNextPrograms.CONTENT_URI, values)
                else resolver.update(TvContractCompat.buildWatchNextProgramUri(old.id), values, null, null)
            }
            for ((id, old) in existing) if (id !in wanted) resolver.delete(TvContractCompat.buildWatchNextProgramUri(old.id), null, null)
        }.onFailure { Diagnostics.log("WatchNext", "sync failed", it) }
    }

    private fun program(p: Progress): WatchNextProgram {
        val episode = p.label != p.meta.name
        return WatchNextProgram.Builder()
            .setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE)
            .setType(if (episode) TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE else TvContractCompat.PreviewPrograms.TYPE_MOVIE)
            .setLastEngagementTimeUtcMillis(p.updated)
            .setTitle(p.meta.name)
            .apply { if (episode) setEpisodeTitle(p.label) }
            .setDescription(p.meta.description)
            .apply { p.meta.poster?.let { setPosterArtUri(it.toUri()) } }
            .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3)
            .setLastPlaybackPositionMillis(p.position.toInt())
            .setDurationMillis(p.duration.toInt())
            .setIntentUri(DeepLink.forTitle(p.meta).toUri())
            .setInternalProviderId(p.meta.id)
            .build() as WatchNextProgram
    }

    private companion object {
        const val MAX_ITEMS = 10
    }
}
