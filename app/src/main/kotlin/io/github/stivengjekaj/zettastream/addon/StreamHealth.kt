package io.github.stivengjekaj.zettastream.addon

import io.github.stivengjekaj.zettastream.storage.SafeFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** The streams that did not start, and the times when the streams of each addon did not start. */
@Serializable
data class HealthData(
    val streams: Map<String, Long> = emptyMap(),
    val addons: Map<String, List<Long>> = emptyMap(),
)

/**
 * Remembers the streams that did not start. The app then chooses them last,
 * so that one broken link does not come back each time.
 */
class StreamHealth(private val file: File, private val clock: () -> Long = System::currentTimeMillis) : StreamOrder.Health {
    private val json = Json { ignoreUnknownKeys = true }
    private val writer = SafeFile(file)
    @Volatile private var data: HealthData = read()

    override fun failedRecently(listed: ListedStream): Boolean = failedRecently(key(listed))

    /** True when this stream did not start in the last day. */
    fun failedRecently(key: String): Boolean = data.streams[key]?.let { clock() - it < STREAM_MEMORY } ?: false

    /** True when [ADDON_FAILURES] streams of this addon did not start in the last day, and none started since. */
    override fun unreliable(addonUrl: String): Boolean =
        data.addons[addonUrl].orEmpty().count { clock() - it < STREAM_MEMORY } >= ADDON_FAILURES

    suspend fun failed(key: String, addonUrl: String) {
        val now = clock()
        save(
            data.copy(
                streams = data.streams + (key to now),
                addons = data.addons + (addonUrl to (data.addons[addonUrl].orEmpty() + now).takeLast(ADDON_FAILURES * 2)),
            ),
        )
    }

    /** A stream that plays clears its own mark and the marks of its addon. */
    suspend fun played(key: String, addonUrl: String) {
        if (key !in data.streams && addonUrl !in data.addons) return
        save(data.copy(streams = data.streams - key, addons = data.addons - addonUrl))
    }

    private suspend fun save(next: HealthData) {
        val now = clock()
        // Old marks go away, so the file stays small.
        data = next.copy(
            streams = next.streams.filterValues { now - it < STREAM_MEMORY },
            addons = next.addons.mapValues { (_, times) -> times.filter { now - it < STREAM_MEMORY } }.filterValues { it.isNotEmpty() },
        )
        writer.write(json.encodeToString(HealthData.serializer(), data))
    }

    private fun read(): HealthData = runCatching { json.decodeFromString(HealthData.serializer(), file.readText()) }.getOrDefault(HealthData())

    companion object {
        const val STREAM_MEMORY = 24 * 60 * 60_000L
        const val ADDON_FAILURES = 3

        /** One key for each stream: the addon and the link, or the torrent and its file. */
        fun key(addonUrl: String?, url: String?, infoHash: String?, fileIdx: Int?): String =
            addonUrl.orEmpty() + "|" + (url?.takeIf { it.isNotEmpty() } ?: "${infoHash.orEmpty().lowercase()}:${fileIdx ?: -1}")

        fun key(listed: ListedStream): String =
            key(listed.addon.manifestUrl, listed.stream.url, listed.stream.infoHash, listed.stream.fileIdx)
    }
}
