package io.github.stivengjekaj.zettastream.library

import io.github.stivengjekaj.zettastream.addon.MetaPreview
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LibraryTest {
    @get:Rule val folder = TemporaryFolder()
    private var now = 0L
    private fun library() = Library(folder.root.resolve("library.json")) { now }
    private val show = MetaPreview("tt1", "series", "Show")
    private val film = MetaPreview("tt2", "movie", "Film")

    @Test
    fun theWatchlistTogglesAndStaysAfterARestart() = runTest {
        val library = library()
        library.toggleWatchlist(show)
        assertTrue(library().isInWatchlist("tt1"))
        library.toggleWatchlist(show)
        assertFalse(library().isInWatchlist("tt1"))
    }

    @Test
    fun continueWatchingShowsTheNewestUnfinishedVideoOfEachTitle() = runTest {
        val library = library()
        now = 1; library.saveProgress("tt1:1:1", show, "E1", 100, 1000)
        now = 2; library.saveProgress("tt2", film, "Film", 50, 1000)
        now = 3; library.saveProgress("tt1:1:2", show, "E2", 200, 1000)
        now = 4; library.saveProgress("tt3", MetaPreview("tt3", "movie"), "Done", 950, 1000)
        assertEquals(listOf("tt1:1:2", "tt2"), library.continueWatching().map { it.videoId })
    }

    @Test
    fun aVideoIsWatchedAfterNinetyPercent() = runTest {
        val library = library()
        library.saveProgress("tt2", film, "Film", 899, 1000)
        assertFalse(library.isWatched("tt2"))
        library.saveProgress("tt2", film, "Film", 900, 1000)
        assertTrue(library.isWatched("tt2"))
        library.toggleWatched("tt2")
        assertFalse(library.isWatched("tt2"))
    }

    @Test
    fun aDamagedFileGivesAnEmptyLibrary() {
        folder.root.resolve("library.json").writeText("{not json")
        assertEquals(LibraryData(), library().data.value)
    }

    @Test
    fun markWatchedDoesNotRemoveAMark() = runTest {
        val library = library()
        library.markWatched("tt1:1:1")
        library.markWatched("tt1:1:1")
        assertTrue(library().isWatched("tt1:1:1"))
    }
}
