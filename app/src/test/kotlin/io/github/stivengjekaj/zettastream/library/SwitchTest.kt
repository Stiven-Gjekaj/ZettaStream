package io.github.stivengjekaj.zettastream.library

import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.settings.SettingsStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SwitchTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun eachProfileKeepsItsOwnLibraryAndSettings() = runBlocking {
        val a = folder.root.resolve("profiles/a")
        val b = folder.root.resolve("profiles/b")
        val library = Library(a.resolve("library.json"))
        val settings = SettingsStore(a.resolve("settings.json"))
        library.toggleWatchlist(MetaPreview("tt1", "movie", "One"))
        settings.update { it.copy(directFirst = true) }

        library.switchTo(b.resolve("library.json"))
        settings.switchTo(b.resolve("settings.json"))
        assertEquals(emptyList<String>(), library.data.value.watchlist.map { it.id })
        assertEquals(false, settings.settings.value.directFirst)
        library.toggleWatchlist(MetaPreview("tt2", "movie", "Two"))

        library.switchTo(a.resolve("library.json"))
        settings.switchTo(a.resolve("settings.json"))
        assertEquals(listOf("tt1"), library.data.value.watchlist.map { it.id })
        assertEquals(true, settings.settings.value.directFirst)
        assertEquals(listOf("tt2"), Library(b.resolve("library.json")).data.value.watchlist.map { it.id })
    }
}
