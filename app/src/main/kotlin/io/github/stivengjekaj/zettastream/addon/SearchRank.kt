package io.github.stivengjekaj.zettastream.addon

import java.text.Normalizer

/**
 * Puts search results from many addons in one list: the same title from two addons
 * becomes one entry, and the titles that match the search best come first.
 */
object SearchRank {
    /** A title with the number of addons that gave it. */
    private class Entry(var meta: MetaPreview, var sources: Int, val order: Int)

    /**
     * [answers] has one list for each addon, in the order of the source list.
     * Two entries are one title when they have the same ID or IMDb ID, or the same
     * name, kind, and first year. The entry with an IMDb ID stays, because most
     * stream addons know that ID.
     */
    fun merge(query: String, answers: List<List<MetaPreview>>): List<MetaPreview> {
        val entries = mutableListOf<Entry>()
        var order = 0
        for (answer in answers) {
            for (meta in answer.distinctBy { it.type + it.id }) {
                val same = entries.firstOrNull { sameTitle(it.meta, meta) }
                if (same == null) {
                    entries += Entry(meta, 1, order++)
                } else {
                    same.sources++
                    if (!same.meta.id.startsWith("tt") && imdb(meta) != null && meta.id.startsWith("tt")) same.meta = fill(meta, same.meta)
                    else same.meta = fill(same.meta, meta)
                }
            }
        }
        return entries
            .sortedWith(compareByDescending<Entry> { score(query, it.meta) }.thenByDescending { it.sources }.thenBy { it.order })
            .map { it.meta }
    }

    /** How well the name of [meta] matches [query]: 100 for the same name, then less. */
    fun score(query: String, meta: MetaPreview): Int {
        val q = normalize(query)
        if (q.isEmpty()) return 0
        val words = q.split(' ')
        return (listOf(meta.name) + meta.aliases).maxOf { name ->
            val n = normalize(name)
            val nameWords = n.split(' ')
            when {
                n == q -> 100
                n.startsWith("$q ") -> 80
                words.all { it in nameWords } -> 60
                n.contains(q) -> 40
                words.all { w -> nameWords.any { it.startsWith(w) } } -> 30
                else -> 0
            }
        }
    }

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .lowercase().replace("&", " and ").replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()

    private fun sameTitle(a: MetaPreview, b: MetaPreview): Boolean {
        if (a.id == b.id) return true
        val ia = imdb(a)
        val ib = imdb(b)
        if (ia != null && ib != null) return ia == ib
        return kind(a) == kind(b) && normalize(a.name) == normalize(b.name) &&
            Display.year(a.releaseInfo) != null && Display.year(a.releaseInfo) == Display.year(b.releaseInfo)
    }

    private fun imdb(m: MetaPreview): String? = m.id.takeIf { it.startsWith("tt") } ?: m.imdbId?.takeIf { it.startsWith("tt") }

    /** A film is one kind. A series and an anime series are the other. */
    private fun kind(m: MetaPreview) = if (m.type.equals("movie", ignoreCase = true)) "movie" else "show"

    /** Keeps [kept], with the facts that only [other] has. */
    private fun fill(kept: MetaPreview, other: MetaPreview) = kept.copy(
        poster = kept.poster ?: other.poster,
        background = kept.background ?: other.background,
        logo = kept.logo ?: other.logo,
        description = kept.description ?: other.description,
        releaseInfo = kept.releaseInfo ?: other.releaseInfo,
        imdbRating = kept.imdbRating ?: other.imdbRating,
        genres = kept.genres.ifEmpty { other.genres },
        imdbId = kept.imdbId ?: other.imdbId ?: other.id.takeIf { it.startsWith("tt") },
    )
}
