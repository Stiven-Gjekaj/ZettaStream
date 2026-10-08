package io.github.stivengjekaj.zettastream.addon

import io.github.stivengjekaj.zettastream.source.Source
import io.github.stivengjekaj.zettastream.source.SourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AddonState(
    val addons: List<Addon> = emptyList(),
    val failures: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
)

/** A catalog row on the home screen. */
data class CatalogRow(val addon: Addon, val catalog: Catalog) {
    val key: String get() = "${addon.manifestUrl}|${catalog.type}|${catalog.id}"
}

/** The streams of one addon, or the reason that it gave none. */
data class StreamGroup(
    val addon: Addon,
    val streams: List<Stream> = emptyList(),
    val hidden: Int = 0,
    val error: String? = null,
)

/** Installs the addons in the source list and sends requests to all of them. */
class AddonRepository(
    private val client: AddonClient,
    sources: StateFlow<List<Source>>,
    scope: CoroutineScope,
) {
    private val state = MutableStateFlow(AddonState(loading = true))
    val addons: StateFlow<AddonState> = state.asStateFlow()
    private val retries = MutableStateFlow(0)

    init {
        scope.launch {
            combine(sources, retries) { list, _ -> list }
                .collectLatest { list -> install(list.filter { it.kind == SourceKind.Addon }) }
        }
    }

    /** Installs each addon again, for example after the network comes back. */
    fun retry() {
        retries.value++
    }

    private suspend fun install(sources: List<Source>) = coroutineScope {
        state.value = state.value.copy(loading = true)
        val results = sources.map { source ->
            async { source.url to runCatching { client.install(source.url) } }
        }.awaitAll()
        state.value = AddonState(
            addons = results.mapNotNull { it.second.getOrNull() },
            failures = results.mapNotNull { (url, r) -> r.exceptionOrNull()?.let { url to (it.message ?: it.javaClass.simpleName) } }.toMap(),
            loading = false,
        )
    }

    fun homeRows(): List<CatalogRow> = state.value.addons.flatMap { addon ->
        addon.manifest.catalogs.filterNot { it.needsExtra }.map { CatalogRow(addon, it) }
    }

    /** The types that the home screen can filter by, in a fixed order. */
    fun types(): List<String> {
        val order = listOf("movie", "series", "anime", "tv", "channel")
        return homeRows().map { it.catalog.type }.distinct().sortedBy { order.indexOf(it).let { i -> if (i < 0) 99 else i } }
    }

    suspend fun catalog(row: CatalogRow): List<MetaPreview> = client.catalog(row.addon, row.catalog)

    /** Searches each catalog that supports a search. Results come as they arrive. */
    fun search(query: String): Flow<List<MetaPreview>> = channelFlow {
        val found = mutableListOf<MetaPreview>()
        val catalogs = state.value.addons.flatMap { a -> a.manifest.catalogs.filter { it.supportsSearch }.map { a to it } }
        catalogs.map { (addon, catalog) ->
            launch {
                val metas = runCatching { client.catalog(addon, catalog, mapOf("search" to query)) }.getOrDefault(emptyList())
                synchronized(found) {
                    found += metas.filter { m -> found.none { it.id == m.id } }
                    trySend(found.toList())
                }
            }
        }.forEach { it.join() }
        send(found.toList())
    }

    /** Gets the full details from the first addon that has them. */
    suspend fun meta(preview: MetaPreview): Meta {
        for (addon in state.value.addons.filter { it.supports("meta", preview.type, preview.id) }) {
            val meta = runCatching { client.meta(addon, preview.type, preview.id) }.getOrNull()
            if (meta != null) return meta
        }
        return Meta(
            id = preview.id, type = preview.type, name = preview.name, poster = preview.poster,
            background = preview.background, logo = preview.logo, description = preview.description,
            releaseInfo = preview.releaseInfo, imdbRating = preview.imdbRating, genres = preview.genres,
        )
    }

    /** Asks each addon for streams. Each group comes when its addon answers. */
    fun streams(type: String, videoId: String): Flow<StreamGroup> = channelFlow {
        state.value.addons.filter { it.supports("stream", type, videoId) }.forEach { addon ->
            launch {
                val group = runCatching { client.streams(addon, type, videoId) }.fold(
                    onSuccess = { all ->
                        val playable = all.filter { it.isPlayable }
                        StreamGroup(addon, playable, hidden = all.size - playable.size)
                    },
                    onFailure = { StreamGroup(addon, error = it.message ?: it.javaClass.simpleName) },
                )
                send(group)
            }
        }
    }

    /** Asks one addon for the playable streams of a video. Null when the addon is not installed. */
    suspend fun streamsFrom(addonUrl: String, type: String, videoId: String): List<Stream>? {
        val addon = state.value.addons.firstOrNull { it.manifestUrl == addonUrl } ?: return null
        return runCatching { client.streams(addon, type, videoId) }.getOrDefault(emptyList()).filter { it.isPlayable }
    }

    suspend fun subtitles(type: String, videoId: String): List<Subtitle> = coroutineScope {
        state.value.addons.filter { it.supports("subtitles", type, videoId) }.map { addon ->
            async { runCatching { client.subtitles(addon, type, videoId) }.getOrDefault(emptyList()) }
        }.awaitAll().flatten().distinctBy { it.url }
    }
}

/**
 * Selects the stream of the next episode that comes from the same source as
 * the current one. The addon marks streams of one source with a binge group.
 * Without one, a stream with the same name is the best match.
 */
fun pickSameSource(streams: List<Stream>, bingeGroup: String?, streamName: String?): Stream? =
    streams.firstOrNull { bingeGroup != null && it.behaviorHints?.bingeGroup == bingeGroup }
        ?: streams.firstOrNull { streamName != null && it.name == streamName }
        ?: streams.firstOrNull()
