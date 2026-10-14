package io.github.stivengjekaj.zettastream.addon

/** Finds a catalog for the "More like this" row of a title. */
object Similar {
    /**
     * The first catalog of the same type that can filter by one of the [genres] of the title.
     * A catalog where the genre is a choice comes before one where it is required, because a
     * required genre is often a list of the newest titles only. A catalog that needs other
     * values, such as a search, is left out.
     */
    fun row(addons: List<Addon>, type: String, genres: List<String>): CatalogRow? {
        val candidates = addons.flatMap { a -> a.manifest.catalogs.map { a to it } }
            .filter { (_, c) -> c.type.equals(type, ignoreCase = true) }
            .filter { (_, c) -> c.extras.none { it.isRequired && it.name != "genre" } }
            .sortedBy { (_, c) -> if (c.extras.any { it.name == "genre" && it.isRequired }) 1 else 0 }
        for (genre in genres) {
            for ((addon, catalog) in candidates) {
                val option = catalog.extras.firstOrNull { it.name == "genre" }?.options?.firstOrNull { it.equals(genre, ignoreCase = true) }
                if (option != null) return CatalogRow(addon, catalog, mapOf("genre" to option))
            }
        }
        return null
    }
}
