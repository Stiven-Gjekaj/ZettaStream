package io.github.stivengjekaj.zettastream.torrent

import java.net.URLEncoder

/** Pure helpers for the torrent engine. They are separate so that tests can use them. */
object TorrentMath {
    /** Public trackers that help to find peers when the addon gives none. */
    val DefaultTrackers = listOf(
        "udp://tracker.opentrackr.org:1337/announce",
        "udp://open.stealth.si:80/announce",
        "udp://tracker.torrent.eu.org:451/announce",
        "udp://exodus.desync.com:6969/announce",
        "udp://tracker.openbittorrent.com:6969/announce",
        "udp://open.demonii.com:1337/announce",
    )

    private val VideoExtensions = setOf("mkv", "mp4", "m4v", "avi", "webm", "mov", "ts", "m2ts", "wmv", "flv", "mpg", "mpeg")

    fun isInfoHash(text: String?): Boolean = text != null && text.length == 40 && text.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }

    /**
     * Makes a magnet link. Stremio addons give trackers in `sources` as
     * `tracker:udp://...`, and DHT nodes as `dht:...`, which the link leaves out.
     */
    fun magnet(infoHash: String, sources: List<String>, name: String? = null): String {
        val trackers = (sources.filter { it.startsWith("tracker:") }.map { it.removePrefix("tracker:") } + DefaultTrackers).distinct()
        return buildString {
            append("magnet:?xt=urn:btih:").append(infoHash.lowercase())
            if (!name.isNullOrBlank()) append("&dn=").append(URLEncoder.encode(name, "UTF-8"))
            trackers.forEach { append("&tr=").append(URLEncoder.encode(it, "UTF-8")) }
        }
    }

    /**
     * Selects the file to play, in this order: the index that the addon gave
     * when it is a video, the file with the name that the addon gave, the
     * video with the episode number in its name, and the largest video.
     * A season pack often has no index, so the name and the episode matter.
     */
    fun chooseFile(
        names: List<String>,
        sizes: List<Long>,
        preferred: Int?,
        filename: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ): Int {
        if (names.isEmpty()) return -1
        if (preferred != null && preferred in names.indices && isVideo(names[preferred])) return preferred
        val videos = names.indices.filter { isVideo(names[it]) }
        if (filename != null) {
            val wanted = filename.substringAfterLast('/').lowercase()
            videos.firstOrNull { names[it].substringAfterLast('/').lowercase() == wanted }?.let { return it }
        }
        if (episode != null) {
            val matches = videos.filter { matchesEpisode(names[it], season, episode) }
            if (matches.isNotEmpty()) return matches.maxBy { sizes[it] }
        }
        return (videos.ifEmpty { names.indices.toList() }).maxBy { sizes[it] }
    }

    /** Finds S01E02, 1x02, or " - 02" (the anime form) in a file name. */
    fun matchesEpisode(name: String, season: Int?, episode: Int): Boolean {
        val n = name.substringAfterLast('/')
        val e = "0*$episode"
        val patterns = buildList {
            if (season != null) {
                add(Regex("""(?i)s0*${season}[ ._-]*e$e(?!\d)"""))
                add(Regex("""(?i)(?<!\d)0*${season}x$e(?!\d)"""))
            }
            add(Regex("""(?i)(?:\s-\s|\be|\bep\.?\s?|episode\s)$e(?!\d)"""))
        }
        return patterns.any { it.containsMatchIn(n) }
    }

    /**
     * Selects the pieces to download: the head and the tail of the file,
     * which a player reads first, and a window ahead of the read position.
     * The engine downloads nothing else, so it does not take the whole file.
     */
    fun wanted(firstPiece: Int, lastPiece: Int, readPiece: Int, aheadPieces: Int, edgePieces: Int): Set<Int> {
        if (lastPiece < firstPiece) return emptySet()
        val head = firstPiece..minOf(lastPiece, firstPiece + edgePieces - 1)
        val tail = maxOf(firstPiece, lastPiece - edgePieces + 1)..lastPiece
        val start = readPiece.coerceIn(firstPiece, lastPiece)
        val window = start..minOf(lastPiece, start + aheadPieces)
        return (head + tail + window).toSet()
    }

    /** The number of pieces in [bytes], at least one. */
    fun piecesFor(bytes: Long, pieceLength: Int): Int = maxOf(1, ((bytes + pieceLength - 1) / pieceLength).toInt())

    fun isVideo(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in VideoExtensions

    fun mimeType(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mkv" -> "video/x-matroska"
        "mp4", "m4v", "mov" -> "video/mp4"
        "webm" -> "video/webm"
        "avi" -> "video/x-msvideo"
        "ts", "m2ts" -> "video/mp2t"
        else -> "application/octet-stream"
    }

    /**
     * Reads an HTTP `Range` header such as `bytes=100-` or `bytes=100-199`.
     * Gives the whole file when there is no header, and null when the range
     * is outside the file.
     */
    fun range(header: String?, size: Long): LongRange? {
        if (size <= 0) return null
        if (header == null || !header.startsWith("bytes=")) return 0 until size
        val spec = header.removePrefix("bytes=").substringBefore(',').trim()
        val start = spec.substringBefore('-').trim()
        val end = spec.substringAfter('-', "").trim()
        return when {
            start.isEmpty() && end.isNotEmpty() -> {
                // A suffix range: the last N bytes.
                val n = end.toLongOrNull() ?: return null
                (size - n).coerceAtLeast(0) until size
            }
            else -> {
                val s = start.toLongOrNull() ?: return null
                val e = end.toLongOrNull()?.coerceAtMost(size - 1) ?: (size - 1)
                if (s >= size || s > e) null else s..e
            }
        }
    }
}
