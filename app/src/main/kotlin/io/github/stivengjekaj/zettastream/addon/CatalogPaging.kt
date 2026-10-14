package io.github.stivengjekaj.zettastream.addon

/** Loads a catalog one page at a time, with the `skip` value of Stremio addons. */
object CatalogPaging {
    /** True when the catalog accepts `skip`, so it can give more than its first page. */
    fun canPage(row: CatalogRow): Boolean = row.catalog.extras.any { it.name == "skip" }

    /** The row for the page after [loaded] titles. */
    fun page(row: CatalogRow, loaded: Int): CatalogRow =
        if (loaded == 0) row else row.copy(extras = row.extras + ("skip" to loaded.toString()))

    /**
     * Adds a new page to the titles. A title that is there already does not come twice.
     * There is more only when the page gave at least one new title.
     */
    fun add(titles: List<MetaPreview>, page: List<MetaPreview>): Pair<List<MetaPreview>, Boolean> {
        val known = titles.mapTo(HashSet()) { it.type + it.id }
        val fresh = page.filter { known.add(it.type + it.id) }
        return (titles + fresh) to fresh.isNotEmpty()
    }
}
