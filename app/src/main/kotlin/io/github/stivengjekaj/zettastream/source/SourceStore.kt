package io.github.stivengjekaj.zettastream.source

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Keeps the source list in the private storage of the app. A configured addon
 * URL can hold a key, so the file is never shared and never goes to a backup.
 */
class SourceStore(private val file: File) {
    private val state = MutableStateFlow(read())
    val sources: StateFlow<List<Source>> = state.asStateFlow()

    /** Replaces the full list. A line that the user removed also removes its source. */
    suspend fun replace(text: String): List<Source> {
        val sources = SourceList.parse(text)
        withContext(Dispatchers.IO) {
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(SourceList.format(sources))
            temp.renameTo(file)
        }
        state.value = sources
        return sources
    }

    private fun read(): List<Source> =
        if (file.exists()) SourceList.parse(file.readText()) else emptyList()
}
