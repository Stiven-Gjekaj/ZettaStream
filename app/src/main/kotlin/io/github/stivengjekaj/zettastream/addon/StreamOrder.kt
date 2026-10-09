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
    fun best(groups: List<StreamGroup>, preferDirect: Boolean): ListedStream? {
        val all = groups.flatMap { g -> g.streams.map { ListedStream(g.addon, it, StreamInfo.of(it)) } }
        return all.withIndex().minWithOrNull(
            compareBy<IndexedValue<ListedStream>> { resolutionRank(it.value.info.resolution) }
                .thenBy { val s = it.value; s.stream.isTorrent && (s.info.seeders ?: 0) < FEW_SEEDERS }
                .thenBy { it.value.stream.isTorrent == preferDirect }
                .thenByDescending { if (it.value.stream.isTorrent) it.value.info.seeders ?: 0 else 0 }
                .thenBy { it.index },
        )?.value
    }

    /** 0 for 1080p. Lower resolutions rank before higher ones. An unknown resolution ranks last. */
    private fun resolutionRank(resolution: Int?): Int = when {
        resolution == null -> Int.MAX_VALUE
        resolution == TARGET_RESOLUTION -> 0
        resolution < TARGET_RESOLUTION -> TARGET_RESOLUTION - resolution
        else -> 100_000 + resolution
    }
}
