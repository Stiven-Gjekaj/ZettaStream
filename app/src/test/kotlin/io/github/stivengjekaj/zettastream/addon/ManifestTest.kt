package io.github.stivengjekaj.zettastream.addon

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManifestTest {
    private fun parse(text: String) = Manifest.parse(Json.parseToJsonElement(text).jsonObject)

    @Test
    fun readsResourcesAsNamesAndAsObjects() {
        val manifest = parse(
            """
            {"id":"a","name":"A","types":["movie","series"],"idPrefixes":["tt"],
             "resources":["catalog",{"name":"stream","types":["series"],"idPrefixes":["kitsu:"]}],
             "catalogs":[]}
            """,
        )
        assertEquals(listOf("catalog", "stream"), manifest.resources.map { it.name })
        assertEquals(listOf("series"), manifest.resources[1].types)
    }

    @Test
    fun readsTheNewAndTheOldFormOfCatalogExtras() {
        val manifest = parse(
            """
            {"id":"a","name":"A","resources":[],"types":[],
             "catalogs":[
               {"type":"movie","id":"top","name":"Top","extra":[{"name":"search","isRequired":true},{"name":"genre","options":["Drama"]}]},
               {"type":"series","id":"old","extraSupported":["search","skip"],"extraRequired":["search"]}
             ]}
            """,
        )
        val (top, old) = manifest.catalogs
        assertTrue(top.supportsSearch)
        assertTrue(top.needsExtra)
        assertEquals(listOf("Drama"), top.extras[1].options)
        assertTrue(old.supportsSearch)
        assertTrue(old.needsExtra)
        assertEquals("old", old.name)
    }

    @Test
    fun readsTheConfigurationRequiredHint() {
        val manifest = parse("""{"id":"a","behaviorHints":{"configurationRequired":true}}""")
        assertTrue(manifest.configurationRequired)
    }

    @Test
    fun supportsChecksTheResourceTheTypeAndThePrefix() {
        val addon = Addon(
            "https://x.test/manifest.json",
            parse(
                """
                {"id":"a","types":["movie","series"],"idPrefixes":["tt"],
                 "resources":["meta",{"name":"stream","types":["series"],"idPrefixes":["kitsu:"]}]}
                """,
            ),
        )
        assertTrue(addon.supports("meta", "movie", "tt1"))
        assertFalse(addon.supports("meta", "movie", "kitsu:1"))
        assertTrue(addon.supports("stream", "series", "kitsu:1"))
        assertFalse(addon.supports("stream", "movie", "kitsu:1"))
        assertFalse(addon.supports("subtitles", "movie", "tt1"))
    }
}
