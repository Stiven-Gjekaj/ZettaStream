package io.github.stivengjekaj.zettastream.addon

/** One stream in the list, with the addon that gave it. */
data class ListedStream(val addon: Addon, val stream: Stream, val info: StreamInfo)

/** A part of the list with a heading. */
data class StreamSection(val title: String, val streams: List<ListedStream>)

object StreamOrder {
    /**
     * Puts the streams in order. Torrents are sorted by seeders. When
     * [directFirst] is on, all direct links come first, then all torrents.
     * Otherwise each addon keeps its own part of the list, in the order of
     * the source list.
     */
    fun sections(groups: List<StreamGroup>, directFirst: Boolean): List<StreamSection> {
        val bySeeders = compareByDescending<ListedStream> { it.info.seeders ?: -1 }
        fun listed(g: StreamGroup) = g.streams.map { ListedStream(g.addon, it, StreamInfo.of(it)) }
        return if (directFirst) {
            val all = groups.flatMap(::listed)
            listOf(
                StreamSection("Direct links", all.filter { !it.stream.isTorrent }),
                StreamSection("Torrents", all.filter { it.stream.isTorrent }.sortedWith(bySeeders)),
            ).filter { it.streams.isNotEmpty() }
        } else {
            groups.filter { it.streams.isNotEmpty() }.map { g ->
                val items = listed(g)
                StreamSection(g.addon.name, items.filter { !it.stream.isTorrent } + items.filter { it.stream.isTorrent }.sortedWith(bySeeders))
            }
        }
    }

    /** The resolution that [best] looks for first. */
    const val TARGET_RESOLUTION = 1080

    /** A torrent with fewer known seeders than this is a poor choice. */
    private const val FEW_SEEDERS = 3

    /**
     * Chooses the stream to play at once. A 1080p stream comes first. With
     * no 1080p stream, the nearest lower resolution comes next, then a higher
     * one, then a stream with no known resolution. At the same resolution, the
     * preferred kind comes first: direct links when [preferDirect] is on,
     * torrents when it is off. A torrent with almost no seeders comes after
     * the other streams of its resolution. Then torrents sort by seeders, and
     * direct links keep the order of the source list.
     */
    fun best(groups: List<StreamGroup>, preferDirect: Boolean, maxTorrentBytes: Long? = null, health: Health = Health.None): ListedStream? =
        ranked(groups, preferDirect, maxTorrentBytes, health).firstOrNull()

    /** What the app remembers of streams that did not start. */
    interface Health {
        fun failedRecently(listed: ListedStream): Boolean
        fun unreliable(addonUrl: String): Boolean

        object None : Health {
            override fun failedRecently(listed: ListedStream) = false
            override fun unreliable(addonUrl: String) = false
        }
    }

    /**
     * The largest torrent that streams well: about 7 Mbit/s for a two hour film
     * or a 45 minute episode. A larger file needs more speed than most peers give,
     * and the player stops again and again.
     */
    fun streamableBytes(type: String): Long =
        if (type.trim().lowercase() in setOf("movie", "movies", "film")) 6L * GB else 5L * GB / 2

    private const val GB = 1024L * 1024 * 1024

    private fun tooBig(listed: ListedStream, maxTorrentBytes: Long?): Boolean =
        maxTorrentBytes != null && listed.stream.isTorrent && (listed.info.sizeBytes ?: 0) > maxTorrentBytes

    /** All streams, best first, in the order that [best] uses. The player tries the next one when a stream fails. */
    fun ranked(groups: List<StreamGroup>, preferDirect: Boolean, maxTorrentBytes: Long? = null, health: Health = Health.None): List<ListedStream> {
        val all = groups.flatMap { g -> g.streams.map { ListedStream(g.addon, it, StreamInfo.of(it)) } }
        return all.withIndex().sortedWith(
            // A stream that did not start in the last day comes last. An addon with many such streams comes after the others.
            compareBy<IndexedValue<ListedStream>> { health.failedRecently(it.value) }
                .thenBy { resolutionRank(it.value.info.resolution) }
                .thenBy { health.unreliable(it.value.addon.manifestUrl) }
                .thenBy { tooBig(it.value, maxTorrentBytes) }
                .thenBy { val s = it.value; s.stream.isTorrent && (s.info.seeders ?: 0) < FEW_SEEDERS }
                .thenBy { it.value.stream.isTorrent == preferDirect }
                .thenByDescending { if (it.value.stream.isTorrent) it.value.info.seeders ?: 0 else 0 }
                .thenBy { it.index },
        ).map { it.value }
    }

    /** True for a stream that [best] cannot beat: 1080p, of the preferred kind, and not a torrent with almost no seeders. */
    fun isIdeal(listed: ListedStream, preferDirect: Boolean, maxTorrentBytes: Long? = null): Boolean =
        listed.info.resolution == TARGET_RESOLUTION && listed.stream.isTorrent != preferDirect && !tooBig(listed, maxTorrentBytes) &&
            !(listed.stream.isTorrent && (listed.info.seeders ?: 0) < FEW_SEEDERS)

    /**
     * A short name for the stream that plays, such as "MultiMoviesProvider · 1080p · 1.4 GB".
     * The first line of the stream name loses its emoji and its repeated resolution.
     */
    fun shortLabel(listed: ListedStream): String {
        val first = listed.stream.name?.lineSequence()?.firstOrNull().orEmpty()
        val name = first.split('•', '|').first()
            .replace(Regex("[^\\p{L}\\p{N} .,:+&'()\\[\\]-]"), " ")
            .replace(Regex("\\b(2160|1080|720|480)p\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ").trim().take(32)
            .ifEmpty { listed.addon.name }
        val parts = listOfNotNull(
            name,
            if (listed.stream.isTorrent) "Torrent" else null,
            listed.info.resolution?.let { if (it >= 2160) "4K" else "${it}p" },
            listed.info.sizeBytes?.let { StreamInfo.formatSize(it) },
        )
        return parts.joinToString(" · ")
    }

    /** 0 for 1080p. Lower resolutions rank before higher ones. An unknown resolution ranks last. */
    private fun resolutionRank(resolution: Int?): Int = when {
        resolution == null -> Int.MAX_VALUE
        resolution == TARGET_RESOLUTION -> 0
        resolution < TARGET_RESOLUTION -> TARGET_RESOLUTION - resolution
        else -> 100_000 + resolution
    }
}
