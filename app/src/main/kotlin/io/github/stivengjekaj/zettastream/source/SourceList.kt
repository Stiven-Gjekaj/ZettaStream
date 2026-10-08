package io.github.stivengjekaj.zettastream.source

/** The kinds of source that one line can hold. */
enum class SourceKind { Addon, Playlist, Guide }

data class Source(val url: String, val kind: SourceKind)

/** Reads the text that the user pastes. One line holds one URL. */
object SourceList {
    fun parse(text: String): List<Source> = text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { if (it.startsWith("stremio://")) "https://" + it.removePrefix("stremio://") else it }
        .filter { it.startsWith("http://") || it.startsWith("https://") }
        .distinct()
        .map { Source(it, kindOf(it)) }
        .toList()

    fun format(sources: List<Source>): String = sources.joinToString("\n") { it.url }

    private fun kindOf(url: String): SourceKind {
        val path = url.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith("/manifest.json") -> SourceKind.Addon
            path.endsWith(".xml") || path.endsWith(".xml.gz") || path.contains("epg") -> SourceKind.Guide
            else -> SourceKind.Playlist
        }
    }
}
