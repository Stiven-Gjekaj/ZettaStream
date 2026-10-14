package io.github.stivengjekaj.zettastream.library

import io.github.stivengjekaj.zettastream.settings.SettingsStore
import io.github.stivengjekaj.zettastream.settings.ViewerSettings
import io.github.stivengjekaj.zettastream.source.SourceList
import io.github.stivengjekaj.zettastream.source.SourceStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** All that the viewer keeps on the device, in one file. */
@Serializable
data class BackupData(
    val app: String = "ZettaStream",
    val version: Int = 1,
    val library: LibraryData = LibraryData(),
    val settings: ViewerSettings? = null,
    val sources: String? = null,
)

/**
 * Makes a backup of the library, the settings, and the source list, and
 * restores one. The phone page sends the file, so a new install or a new TV
 * keeps the history of the viewer.
 */
class Backup(private val library: Library, private val settings: SettingsStore, private val sources: SourceStore) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun export(): String = json.encodeToString(
        BackupData.serializer(),
        BackupData(library = library.data.value, settings = settings.settings.value, sources = SourceList.format(sources.sources.value)),
    )

    /** Replaces the data on the device with [text]. Gives a short report, or an error for a file that is not a backup. */
    suspend fun restore(text: String): Result<String> = runCatching {
        val data = json.decodeFromString(BackupData.serializer(), text)
        require(data.app == "ZettaStream") { "This file is not a ZettaStream backup" }
        library.replace(data.library)
        data.settings?.let { restored -> settings.update { restored } }
        data.sources?.let { sources.replace(it) }
        fun count(n: Int, word: String) = "$n $word" + if (n == 1) "" else "s"
        "Restored ${count(data.library.watchlist.size, "title")} in the watchlist, ${count(data.library.progress.size, "video")} in the history, " +
            "and ${count(data.sources?.let { SourceList.parse(it).size } ?: 0, "source")}."
    }
}
