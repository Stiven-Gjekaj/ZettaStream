package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeRowsTest {
    private fun addon(name: String, vararg catalogs: Catalog) =
        Addon("https://$name.test/manifest.json", Manifest(id = name, name = name, catalogs = catalogs.toList()))

    private val cinemeta = addon(
        "cinemeta",
        Catalog("movie", "top", "Popular", listOf(CatalogExtra("genre", options = listOf("Action")))),
        Catalog("movie", "year", "New", listOf(CatalogExtra("genre", isRequired = true, options = listOf("2026", "2025")))),
        Catalog("series", "last", "Last videos", listOf(CatalogExtra("lastVideosIds", isRequired = true))),
        Catalog("movie", "find", "Search", listOf(CatalogExtra("search", isRequired = true))),
    )

    @Test
    fun aRequiredChoiceTakesTheFirstOptionAndFreeTextGetsNoRow() {
        val rows = HomeRows.of(cinemeta)
        assertEquals(listOf("Popular", "New 2026"), rows.map { it.title })
        assertEquals(mapOf("genre" to "2026"), rows[1].extras)
        assertTrue(rows[1].key.endsWith("|genre=2026"))
    }

    @Test
    fun newRowsComeFirstAndTheAddonOrderStaysInsideATier() {
        val anime = addon("anime", Catalog("anime", "rated", "Top Rated"), Catalog("anime", "airing", "Currently Airing"), Catalog("anime", "x", "Genres"))
        val ordered = HomeRows.order(HomeRows.of(cinemeta) + HomeRows.of(anime))
        assertEquals(listOf("New 2026", "Currently Airing", "Popular", "Top Rated", "Genres"), ordered.map { it.title })
    }

    @Test
    fun aRowThatRepeatsARowAboveIsARepeat() {
        val above = listOf(listOf("a", "b", "c", "d", "e"))
        assertTrue(HomeRows.isRepeat(listOf("a", "b", "c", "d", "x"), above))
        assertFalse(HomeRows.isRepeat(listOf("a", "x", "y", "z", "w"), above))
        assertFalse(HomeRows.isRepeat(listOf("a", "b"), above))
    }
}
