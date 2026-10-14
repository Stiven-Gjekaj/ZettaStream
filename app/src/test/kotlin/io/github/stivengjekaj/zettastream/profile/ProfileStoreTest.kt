package io.github.stivengjekaj.zettastream.profile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfileStoreTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun theFirstStartMovesTheOldFilesIntoTheFirstProfile() {
        folder.newFile("library.json").writeText("""{"watchlist":[]}""")
        folder.newFile("settings.json").writeText("{}")
        val store = ProfileStore(folder.root)
        val first = store.profiles.value.profiles.single()
        assertEquals(ProfileStore.FIRST_NAME, first.name)
        assertEquals(first.id, store.profiles.value.last)
        assertTrue(store.folder(first.id).resolve("library.json").exists())
        assertFalse(folder.root.resolve("library.json").exists())
        assertEquals(first, ProfileStore(folder.root).profiles.value.profiles.single())
    }

    @Test
    fun aProfileCanBeAddedEditedAndDeletedButNotTheLast() = runBlocking {
        val store = ProfileStore(folder.root)
        val first = store.profiles.value.profiles.single()
        val kid = store.add("  Kid  ", "https://a.test/a.png")
        assertEquals("Kid", kid.name)
        store.edit(kid.id, "", null)
        assertEquals(Profile(kid.id, "Profile", null), store.profiles.value.profiles.last())
        store.select(kid.id)
        store.folder(kid.id).mkdirs()
        assertTrue(store.delete(kid.id))
        assertFalse(store.folder(kid.id).exists())
        assertEquals(null, store.profiles.value.last)
        assertFalse(store.delete(first.id))
        assertEquals(listOf(first), ProfileStore(folder.root).profiles.value.profiles)
    }
}
