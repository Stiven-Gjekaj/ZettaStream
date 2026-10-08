package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.addon.MetaPreview
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTest {
    @Test
    fun theFiveMenuScreensAreTopLevel() {
        // The fault was on Android only: ART left Home out of a list in the companion
        // object. The JVM does not reproduce it, so this test checks the result only.
        listOf(Screen.Home, Screen.Search, Screen.Live, Screen.Library, Screen.Settings)
            .forEach { assertTrue("$it", it.isTopLevel) }
    }

    @Test
    fun otherScreensAreNotTopLevel() {
        assertFalse(Screen.Sources.isTopLevel)
        assertFalse(Screen.Detail(MetaPreview("tt1", "movie")).isTopLevel)
    }
}
