package io.github.stivengjekaj.zettastream.settings

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun aNewStoreHasTheDefaults() {
        val settings = SettingsStore(folder.root.resolve("settings.json")).settings.value
        assertTrue(settings.showTorrents)
        assertFalse(settings.torrentUpload)
        assertTrue(settings.vpnNotice)
    }

    @Test
    fun aChangeStaysAfterARestart() = runTest {
        val file = folder.root.resolve("settings.json")
        SettingsStore(file).update { it.copy(subtitlesOn = true, subtitleSize = SubtitleSize.Large) }
        val again = SettingsStore(file).settings.value
        assertTrue(again.subtitlesOn)
        assertEquals(SubtitleSize.Large, again.subtitleSize)
    }

    @Test
    fun aFileFromAnOlderVersionKeepsItsValues() {
        val file = folder.root.resolve("settings.json")
        file.writeText("""{"subtitlesOn":true,"removedKey":1}""")
        val settings = SettingsStore(file).settings.value
        assertTrue(settings.subtitlesOn)
        assertTrue(settings.autoplayNext)
    }

    @Test
    fun nextGoesRoundTheOptions() {
        val options = SubtitleSize.entries
        assertEquals(SubtitleSize.Large, SettingsStore.next(options, SubtitleSize.Normal))
        assertEquals(SubtitleSize.Small, SettingsStore.next(options, SubtitleSize.Huge))
    }
}
