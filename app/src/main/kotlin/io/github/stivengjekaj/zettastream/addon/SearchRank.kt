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
     * stream addons know that ID. Each result is looked up in tables, not compared
     * with each other result, so a long list stays fast.
     */
    fun merge(query: String, answers: List<List<MetaPreview>>): List<MetaPreview> {
        val names = HashMap<String, String>()
        fun norm(text: String) = names.getOrPut(text) { normalize(text) }
        val entries = mutableListOf<Entry>()
        val byId = HashMap<String, Entry>()
        val byImdb = HashMap<String, Entry>()
        val byName = HashMap<String, Entry>()
        fun nameKey(m: MetaPreview) = Display.year(m.releaseInfo)?.let { "${kind(m)}|${norm(m.name)}|$it" }
        fun index(entry: Entry, m: MetaPreview) {
            byId[m.id] = entry
            imdb(m)?.let { byImdb.putIfAbsent(it, entry) }
            nameKey(m)?.let { byName.putIfAbsent(it, entry) }
        }
        for (answer in answers) {
            for (meta in answer.distinctBy { it.type + it.id }) {
                val i = imdb(meta)
                val byNameMatch = nameKey(meta)?.let { byName[it] }
                // A match by name only counts when the IMDb IDs do not say that the titles differ.
                val same = byId[meta.id] ?: i?.let { byImdb[it] }
                    ?: byNameMatch?.takeIf { e -> imdb(e.meta).let { it == null || i == null } }
                if (same == null) {
                    val entry = Entry(meta, 1, entries.size)
                    entries += entry
                    index(entry, meta)
                } else {
                    same.sources++
                    if (!same.meta.id.startsWith("tt") && meta.id.startsWith("tt")) same.meta = fill(meta, same.meta)
                    else same.meta = fill(same.meta, meta)
                    index(same, meta)
                }
            }
        }
        // The score of each entry is found one time, not in each comparison of the sort.
        val q = norm(query)
        val scored = entries.map { it to score(q, it.meta, ::norm) }
        return scored
            .sortedWith(compareByDescending<Pair<Entry, Int>> { it.second }.thenByDescending { it.first.sources }.thenBy { it.first.order })
            .map { it.first.meta }
    }

    /** How well the name of [meta] matches [query]: 100 for the same name, then less. */
    fun score(query: String, meta: MetaPreview): Int = score(normalize(query), meta, ::normalize)

    private fun score(q: String, meta: MetaPreview, norm: (String) -> String): Int {
        if (q.isEmpty()) return 0
        val words = q.split(' ')
        return (listOf(meta.name) + meta.aliases).maxOf { name ->
            val n = norm(name)
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

    private val marks = Regex("\\p{M}+")
    private val signs = Regex("[^\\p{L}\\p{N}]+")

    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(marks, "")
            .lowercase().replace("&", " and ").replace(signs, " ").trim()

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
