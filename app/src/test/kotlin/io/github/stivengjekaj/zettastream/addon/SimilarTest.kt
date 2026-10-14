package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimilarTest {
    private val cinemeta = Addon(
        "https://c.test/manifest.json",
        Manifest(
            id = "c", name = "C",
            catalogs = listOf(
                Catalog("movie", "year", "New", listOf(CatalogExtra("genre", isRequired = true, options = listOf("2026", "Drama")))),
                Catalog("movie", "top", "Popular", listOf(CatalogExtra("genre", options = listOf("Action", "Drama")), CatalogExtra("search"))),
                Catalog("movie", "find", "Search", listOf(CatalogExtra("search", isRequired = true), CatalogExtra("genre", options = listOf("Crime")))),
                Catalog("series", "top", "Popular", listOf(CatalogExtra("genre", options = listOf("Crime")))),
            ),
        ),
    )

    @Test
    fun aCatalogWithAChosenGenreComesFirst() {
        val row = Similar.row(listOf(cinemeta), "movie", listOf("drama"))!!
        assertEquals("top", row.catalog.id)
        assertEquals(mapOf("genre" to "Drama"), row.extras)
    }

    @Test
    fun theNextGenreIsTriedAndOtherTypesAndSearchesAreLeftOut() {
        assertEquals("Action", Similar.row(listOf(cinemeta), "movie", listOf("Mystery", "Action"))!!.extras["genre"])
        assertNull(Similar.row(listOf(cinemeta), "movie", listOf("Crime")))
        assertNull(Similar.row(listOf(cinemeta), "anime", listOf("Drama")))
    }
}
