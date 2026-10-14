package io.github.stivengjekaj.zettastream.tv

import io.github.stivengjekaj.zettastream.addon.MetaPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkTest {
    @Test
    fun aTitleComesBackFromItsLink() {
        val meta = MetaPreview(id = "kitsu:7442", type = "anime", name = "Attack on Titan & more", poster = "https://img.test/a b.jpg")
        assertEquals(meta, DeepLink.parse(DeepLink.forTitle(meta)))
    }

    @Test
    fun anotherLinkIsNotATitle() {
        assertNull(DeepLink.parse("https://example.test/title?id=1"))
        assertNull(DeepLink.parse("zettastream://title?type=movie"))
    }
}
