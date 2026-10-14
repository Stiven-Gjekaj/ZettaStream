package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPagingTest {
    private val addon = Addon("https://a.test/manifest.json", Manifest(id = "a", name = "A"))
    private val paged = CatalogRow(addon, Catalog("movie", "top", "Popular", listOf(CatalogExtra("genre"), CatalogExtra("skip"))), mapOf("genre" to "Drama"))
    private fun t(id: String) = MetaPreview(id, "movie", id)

    @Test
    fun theNextPageSkipsTheLoadedTitlesAndKeepsTheOtherValues() {
        assertTrue(CatalogPaging.canPage(paged))
        assertEquals(paged, CatalogPaging.page(paged, 0))
        assertEquals(mapOf("genre" to "Drama", "skip" to "48"), CatalogPaging.page(paged, 48).extras)
        assertFalse(CatalogPaging.canPage(CatalogRow(addon, Catalog("movie", "x", "X"))))
    }

    @Test
    fun aPageAddsOnlyNewTitlesAndAnEmptyOrRepeatedPageEnds() {
        val (first, more) = CatalogPaging.add(emptyList(), listOf(t("1"), t("2")))
        assertTrue(more)
        val (second, more2) = CatalogPaging.add(first, listOf(t("2"), t("3")))
        assertEquals(listOf("1", "2", "3"), second.map { it.id })
        assertTrue(more2)
        assertFalse(CatalogPaging.add(second, listOf(t("1"), t("3"))).second)
        assertFalse(CatalogPaging.add(second, emptyList()).second)
    }
}
