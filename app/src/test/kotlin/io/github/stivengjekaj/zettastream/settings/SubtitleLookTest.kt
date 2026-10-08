package io.github.stivengjekaj.zettastream.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleLookTest {
    @Test
    fun theBackgroundOpacityGoesFromNoneToSolid() {
        assertEquals(listOf(0, 64, 128, 191, 255), SubtitleBackground.entries.map { it.alpha })
    }

    @Test
    fun theDefaultsAreWhiteTextWithAnOutlineAndNoBox() {
        val s = ViewerSettings()
        assertEquals(SubtitleColor.White, s.subtitleColor)
        assertEquals(SubtitleBackground.None, s.subtitleBackground)
        assertEquals(SubtitleEdge.Outline, s.subtitleEdge)
    }

    @Test
    fun aFileFromVersion020KeepsTheOldValuesAndGetsTheNewDefaults() {
        val folder = kotlin.io.path.createTempDirectory().toFile()
        val file = folder.resolve("settings.json")
        file.writeText("""{"subtitlesOn":true,"subtitleSize":"Large"}""")
        val s = SettingsStore(file).settings.value
        assertEquals(SubtitleSize.Large, s.subtitleSize)
        assertEquals(SubtitleFont.Default, s.subtitleFont)
        folder.deleteRecursively()
    }
}
