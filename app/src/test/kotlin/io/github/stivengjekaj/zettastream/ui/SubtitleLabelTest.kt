package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.addon.Subtitle
import io.github.stivengjekaj.zettastream.ui.screens.arrangeSubtitles
import io.github.stivengjekaj.zettastream.ui.screens.languageName
import io.github.stivengjekaj.zettastream.ui.screens.languageTag
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

    @Test
    fun theCodesOfOpenSubtitlesBecomeNames() {
        assertEquals("Portuguese (Brazil)", languageName("pob"))
        assertEquals("pt-BR", languageTag("pob"))
        assertEquals("sq", languageTag("sqi"))
        assertEquals("Albanian", languageName("alb"))
        assertEquals("Czech", languageName("cze"))
    }

    @Test
    fun thePreferredLanguageComesFirstWithAFewTracksOfEachLanguage() {
        val subs = (1..5).map { Subtitle(url = "p$it", lang = "pob") } + (1..5).map { Subtitle(url = "e$it", lang = "eng") } + Subtitle(url = "s1", lang = "sqi")
        assertEquals(listOf("e1", "e2", "e3", "p1", "p2", "p3", "s1"), arrangeSubtitles(subs, preferred = "en").map { it.url })
        assertEquals(listOf("p1", "p2", "p3", "e1", "e2", "e3", "s1"), arrangeSubtitles(subs, preferred = "").map { it.url })
    }
}

