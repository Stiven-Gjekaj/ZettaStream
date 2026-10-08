package io.github.stivengjekaj.zettastream.source

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SourceStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun replaceRemovesALineThatIsNotInTheNewList() = runTest {
        val file = folder.newFile("sources.txt")
        val store = SourceStore(file)
        store.replace("https://a.test/manifest.json\nhttps://b.test/manifest.json")
        store.replace("https://b.test/manifest.json")
        assertEquals(listOf("https://b.test/manifest.json"), store.sources.value.map { it.url })
        assertEquals(listOf("https://b.test/manifest.json"), SourceStore(file).sources.value.map { it.url })
    }
}
