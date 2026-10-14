package io.github.stivengjekaj.zettastream.addon

/** How the home screen gets its rows from the catalogs of the addons. */
object HomeRows {
    /**
     * Makes the rows of one addon. A catalog that needs a choice from a list,
     * such as Cinemeta "New" with a year, gets the first choice, which is the
     * newest. A catalog that needs free text, such as a search, gets no row.
     */
    fun of(addon: Addon): List<CatalogRow> = addon.manifest.catalogs.mapNotNull { catalog ->
        val required = catalog.extras.filter { it.isRequired }
        when {
            required.isEmpty() -> CatalogRow(addon, catalog)
            required.all { it.options.isNotEmpty() } ->
                CatalogRow(addon, catalog, required.associate { it.name to it.options.first() })
            else -> null
        }
    }

    private val newWords = Regex("""(?i)\b(new|latest|recent|trending|airing|season|releases?|now|today|live)\b""")
    private val popularWords = Regex("""(?i)\b(popular|top|featured|rated|best)\b""")

    /** 0 for new and trending rows, 1 for popular rows, 2 for the others. */
    fun tier(row: CatalogRow): Int = when {
        newWords.containsMatchIn(row.title) -> 0
        popularWords.containsMatchIn(row.title) -> 1
        else -> 2
    }

    /**
     * Puts the rows in order: by [tier], then in the order of the source list.
     * A sort that is stable keeps the order of the addons inside each tier.
     */
    fun order(rows: List<CatalogRow>): List<CatalogRow> = rows.sortedBy(::tier)

    /**
     * Puts [rows] in the order of [keys]. The rows that [keys] does not name, such as the
     * rows of a new addon, keep their order and come after the others.
     */
    fun arrange(rows: List<CatalogRow>, keys: List<String>): List<CatalogRow> {
        val place = keys.withIndex().associate { (i, k) -> k to i }
        return rows.sortedBy { place[it.key] ?: Int.MAX_VALUE }
    }

    /** The order after the row at [index] moves one place up ([step] -1) or down ([step] 1). */
    fun move(rows: List<CatalogRow>, index: Int, step: Int): List<String> {
        val keys = rows.map { it.key }.toMutableList()
        val target = index + step
        if (index !in keys.indices || target !in keys.indices) return keys
        keys.add(target, keys.removeAt(index))
        return keys
    }

    /**
     * Tells if [items] mostly repeat the items of a row above. Two addons often
     * give the same "Popular" list, and the second copy does not help.
     */
    fun isRepeat(items: List<String>, above: List<List<String>>, limit: Double = 0.7): Boolean {
        if (items.size < 4) return false
        val mine = items.toSet()
        return above.any { other -> other.isNotEmpty() && mine.count { it in other.toSet() } >= mine.size * limit }
    }
}
