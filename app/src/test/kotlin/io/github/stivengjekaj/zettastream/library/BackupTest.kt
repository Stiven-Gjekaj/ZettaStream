package io.github.stivengjekaj.zettastream.library

import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.settings.SettingsStore
import io.github.stivengjekaj.zettastream.source.SourceStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BackupTest {
    @get:Rule val folder = TemporaryFolder()

    private fun device(name: String): Backup {
        val dir = folder.newFolder(name)
        return Backup(Library(dir.resolve("library.json")), SettingsStore(dir.resolve("settings.json")), SourceStore(dir.resolve("sources.txt")))
    }

    @Test
    fun aBackupMovesTheLibraryTheSettingsAndTheSourcesToAnotherDevice() = runBlocking {
        val oldDir = folder.newFolder("old")
        val library = Library(oldDir.resolve("library.json"))
        val settings = SettingsStore(oldDir.resolve("settings.json"))
        val sources = SourceStore(oldDir.resolve("sources.txt"))
        library.toggleWatchlist(MetaPreview(id = "tt1", type = "movie", name = "One"))
        library.toggleFavoriteChannel("news24al")
        settings.update { it.copy(directFirst = true) }
        sources.replace("https://a.test/manifest.json")
        val file = Backup(library, settings, sources).export()

        val newDir = folder.newFolder("new")
        val newLibrary = Library(newDir.resolve("library.json"))
        val newSettings = SettingsStore(newDir.resolve("settings.json"))
        val newSources = SourceStore(newDir.resolve("sources.txt"))
        val report = Backup(newLibrary, newSettings, newSources).restore(file).getOrThrow()

        assertEquals(listOf("tt1"), newLibrary.data.value.watchlist.map { it.id })
        assertEquals(setOf("news24al"), newLibrary.data.value.favoriteChannels)
        assertTrue(newSettings.settings.value.directFirst)
        assertEquals(listOf("https://a.test/manifest.json"), newSources.sources.value.map { it.url })
        assertEquals("Restored 1 title in the watchlist, 0 videos in the history, and 1 source.", report)
    }

    @Test
    fun anotherFileIsRefusedAndChangesNothing() = runBlocking {
        val backup = device("one")
        assertTrue(backup.restore("""{"app":"Other"}""").isFailure)
        assertTrue(backup.restore("not json").isFailure)
    }
}
