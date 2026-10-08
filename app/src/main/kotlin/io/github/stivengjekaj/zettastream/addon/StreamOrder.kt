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
}
