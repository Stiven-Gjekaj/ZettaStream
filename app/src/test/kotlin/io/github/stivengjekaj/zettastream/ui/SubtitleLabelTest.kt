package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.addon.Subtitle
import io.github.stivengjekaj.zettastream.ui.screens.languageName
import io.github.stivengjekaj.zettastream.ui.screens.subtitleLabels
import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleLabelTest {
    @Test
    fun codesBecomeLanguageNames() {
        assertEquals("English", languageName("eng"))
        assertEquals("Albanian", languageName("sq"))
        assertEquals("Subtitle", languageName(""))
        assertEquals("xx-unknown", languageName("xx-unknown"))
    }

    @Test
    fun tracksInOneLanguageAreNumbered() {
        val labels = subtitleLabels(listOf(Subtitle(url = "a", lang = "eng"), Subtitle(url = "b", lang = "sqi"), Subtitle(url = "c", lang = "eng")))
        assertEquals(listOf("English 1", "Albanian", "English 2"), labels.map { it.second })
    }
}
