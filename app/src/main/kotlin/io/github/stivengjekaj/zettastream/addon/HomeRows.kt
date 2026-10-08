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
     * Tells if [items] mostly repeat the items of a row above. Two addons often
     * give the same "Popular" list, and the second copy does not help.
     */
    fun isRepeat(items: List<String>, above: List<List<String>>, limit: Double = 0.7): Boolean {
        if (items.size < 4) return false
        val mine = items.toSet()
        return above.any { other -> other.isNotEmpty() && mine.count { it in other.toSet() } >= mine.size * limit }
    }
}
