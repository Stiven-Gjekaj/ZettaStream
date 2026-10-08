package io.github.stivengjekaj.zettastream.library

import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.storage.SafeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** How far the user watched one video. */
@Serializable
data class Progress(
    val videoId: String,
    val meta: MetaPreview,
    val label: String,
    val position: Long,
    val duration: Long,
    val updated: Long,
) {
    /** A video counts as watched after 90 percent of it. */
    val isFinished: Boolean get() = duration > 0 && position >= duration * 9 / 10
}

@Serializable
data class LibraryData(
    val watchlist: List<MetaPreview> = emptyList(),
    val progress: Map<String, Progress> = emptyMap(),
    val watched: Set<String> = emptySet(),
)

/** Keeps the watchlist and the watch history on the device only. */
class Library(private val file: File, private val clock: () -> Long = System::currentTimeMillis) {
    private val json = Json { ignoreUnknownKeys = true }
    private val writer = SafeFile(file)
    private val state = MutableStateFlow(read())
    val data: StateFlow<LibraryData> = state.asStateFlow()

    fun isInWatchlist(id: String): Boolean = state.value.watchlist.any { it.id == id }

    fun isWatched(id: String): Boolean = id in state.value.watched

    suspend fun toggleWatchlist(meta: MetaPreview) = update { data ->
        if (data.watchlist.any { it.id == meta.id }) data.copy(watchlist = data.watchlist.filterNot { it.id == meta.id })
        else data.copy(watchlist = listOf(meta) + data.watchlist)
    }

    suspend fun markWatched(id: String) = update { it.copy(watched = it.watched + id) }

    suspend fun toggleWatched(id: String) = update { data ->
        if (id in data.watched) data.copy(watched = data.watched - id) else data.copy(watched = data.watched + id)
    }

    suspend fun saveProgress(videoId: String, meta: MetaPreview, label: String, position: Long, duration: Long) =
        update { data ->
            val entry = Progress(videoId, meta, label, position, duration, clock())
            val watched = if (entry.isFinished) data.watched + videoId else data.watched
            data.copy(progress = data.progress + (videoId to entry), watched = watched)
        }

    fun progress(videoId: String): Progress? = state.value.progress[videoId]

    /** The newest unfinished video of each title, newest first. */
    fun continueWatching(): List<Progress> = continueWatching(state.value)

    suspend fun clearHistory() = update { it.copy(progress = emptyMap(), watched = emptySet()) }

    private suspend fun update(change: (LibraryData) -> LibraryData) {
        val next = change(state.value)
        state.value = next
        writer.write(json.encodeToString(LibraryData.serializer(), next))
    }

    private fun read(): LibraryData = runCatching {
        json.decodeFromString(LibraryData.serializer(), file.readText())
    }.getOrDefault(LibraryData())

    companion object {
        fun continueWatching(data: LibraryData): List<Progress> = data.progress.values
            .filter { !it.isFinished && it.position > 0 }
            .sortedByDescending { it.updated }
            .distinctBy { it.meta.id }
    }
}
