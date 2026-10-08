package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Test

class AddonUrlTest {
    private val addon = Addon("https://x.test/config%7Ckey/manifest.json", Manifest(id = "a", name = "A"))

    @Test
    fun theBaseUrlKeepsTheConfigurationPath() {
        assertEquals("https://x.test/config%7Ckey", addon.baseUrl)
    }

    @Test
    fun aStremioLinkBecomesHttps() {
        assertEquals("https://x.test/manifest.json", Addon.normalizeManifestUrl(" stremio://x.test/manifest.json "))
    }

    @Test
    fun theIdIsEncodedLikeEncodeUriComponent() {
        assertEquals(
            "https://x.test/config%7Ckey/stream/series/tt0944947%3A1%3A2.json",
            addon.resourceUrl("stream", "series", "tt0944947:1:2"),
        )
    }

    @Test
    fun extrasAreJoinedAndEncoded() {
        assertEquals(
            "https://x.test/config%7Ckey/catalog/movie/top/search=the%20matrix&skip=100.json",
            addon.resourceUrl("catalog", "movie", "top", linkedMapOf("search" to "the matrix", "skip" to "100")),
        )
    }

    @Test
    fun nonAsciiTextIsEncodedAsUtf8() {
        assertEquals("%C3%A9t%C3%A9", Addon.encodeComponent("été"))
    }
}
