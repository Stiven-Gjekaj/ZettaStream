package io.github.stivengjekaj.zettastream.storage

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SafeFileTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun theLastWriteWins() = runBlocking {
        val file = folder.newFile("settings.json")
        val safe = SafeFile(file)
        safe.write("first")
        safe.write("second")
        assertEquals("second", file.readText())
    }
}
