package io.github.stivengjekaj.zettastream.addon

import io.github.stivengjekaj.zettastream.source.Source
import io.github.stivengjekaj.zettastream.source.SourceKind
import io.github.stivengjekaj.zettastream.ui.typeKey
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
data class CatalogRow(val addon: Addon, val catalog: Catalog, val extras: Map<String, String> = emptyMap()) {
    val key: String get() = "${addon.manifestUrl}|${catalog.type}|${catalog.id}" + extras.entries.joinToString("") { "|${it.key}=${it.value}" }

    /** The name of the row, with the choice it uses, for example "New 2026". */
    val title: String get() = (listOf(catalog.name) + extras.values).joinToString(" ")
}

/** The streams of one addon, or the reason that it gave none. */
data class StreamGroup(
    val addon: Addon,
    val streams: List<Stream> = emptyList(),
    /** Torrent streams that the app hides because the viewer turned torrents off. */
    val hiddenTorrents: Int = 0,
    /** Links that no part of the app can play, such as a YouTube ID or a link to another app. */
    val unplayable: Int = 0,
    val error: String? = null,
)

/** Installs the addons in the source list and sends requests to all of them. */
class AddonRepository(
    private val client: AddonClient,
    sources: StateFlow<List<Source>>,
    scope: CoroutineScope,
    private val fallbackSubtitlesUrl: String? = OPENSUBTITLES,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var fallback: Addon? = null
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

    /** Every row that the addons can give, in the order of [HomeRows.order]. */
    fun homeRows(): List<CatalogRow> = HomeRows.order(state.value.addons.flatMap(HomeRows::of))

    /** The types that the home screen can filter by, in a fixed order. Each kind shows one time. */
    fun types(): List<String> {
        val order = listOf("movie", "series", "anime", "tv", "sport")
        return homeRows().map { typeKey(it.catalog.type) }.distinct().sortedBy { order.indexOf(it).let { i -> if (i < 0) 99 else i } }
    }

    private val catalogCache = java.util.concurrent.ConcurrentHashMap<String, List<MetaPreview>>()

    /** The last answer of a catalog, so that a screen comes back at once after Back. */
    fun cachedCatalog(row: CatalogRow): List<MetaPreview>? = catalogCache[row.key]

    suspend fun catalog(row: CatalogRow): List<MetaPreview> =
        client.catalog(row.addon, row.catalog, row.extras).also { catalogCache[row.key] = it }

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

    /**
     * Asks each addon for streams. Each group comes when its addon answers.
     * Torrent streams come too when [withTorrents] is true.
     */
    private val streamCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Long, List<StreamGroup>>>()

    /**
     * The last full list of streams for a video, so that Back from the player shows
     * the same list at once. A stream link can expire, so the list is kept for
     * [STREAM_KEEP] only.
     */
    fun cachedStreams(type: String, videoId: String, withTorrents: Boolean): List<StreamGroup>? =
        streamCache["$type|$videoId|$withTorrents"]?.takeIf { clock() - it.first < STREAM_KEEP }?.second

    fun keepStreams(type: String, videoId: String, withTorrents: Boolean, groups: List<StreamGroup>) {
        streamCache["$type|$videoId|$withTorrents"] = clock() to groups
    }

    fun streams(type: String, videoId: String, withTorrents: Boolean = false): Flow<StreamGroup> = channelFlow {
        state.value.addons.filter { it.supports("stream", type, videoId) }.forEach { addon ->
            launch {
                val group = runCatching { client.streams(addon, type, videoId) }.fold(
                    onSuccess = { all ->
                        val playable = all.filter { it.isPlayable(withTorrents) }
                        val hiddenTorrents = if (withTorrents) 0 else all.count { it.isTorrent }
                        StreamGroup(addon, playable, hiddenTorrents, all.size - playable.size - hiddenTorrents)
                    },
                    onFailure = { StreamGroup(addon, error = it.message ?: it.javaClass.simpleName) },
                )
                send(group)
            }
        }
    }

    /** Asks one addon for the playable streams of a video. Null when the addon is not installed. */
    suspend fun streamsFrom(addonUrl: String, type: String, videoId: String, withTorrents: Boolean = false): List<Stream>? {
        val addon = state.value.addons.firstOrNull { it.manifestUrl == addonUrl } ?: return null
        return runCatching { client.streams(addon, type, videoId) }.getOrDefault(emptyList()).filter { it.isPlayable(withTorrents) }
    }

    /**
     * Asks each subtitle addon. When no installed addon gives subtitles for
     * this video, the app asks OpenSubtitles, which is free and needs no key.
     */
    suspend fun subtitles(type: String, videoId: String): List<Subtitle> = coroutineScope {
        val installed = state.value.addons.filter { it.supports("subtitles", type, videoId) }
        val addons = installed.ifEmpty { listOfNotNull(fallbackAddon()?.takeIf { it.supports("subtitles", type, videoId) }) }
        addons.map { addon ->
            async { runCatching { client.subtitles(addon, type, videoId) }.getOrDefault(emptyList()) }
        }.awaitAll().flatten().distinctBy { it.url }
    }

    private suspend fun fallbackAddon(): Addon? {
        val url = fallbackSubtitlesUrl ?: return null
        return fallback ?: runCatching { client.install(url) }.getOrNull()?.also { fallback = it }
    }

    companion object {
        /** The official OpenSubtitles addon of Stremio. */
        const val OPENSUBTITLES = "https://opensubtitles-v3.strem.io/manifest.json"

        /** How long a list of streams stays in memory: 15 minutes. */
        const val STREAM_KEEP = 15 * 60_000L
    }
}

/**
 * Selects the stream of the next episode from the same source. A stream in
 * the same binge group comes first, because the addon says that it is the
 * same source. Then the stream whose data is most like the current one:
 * resolution, codec, HDR, release group, and audio. A stream with the same
 * kind (direct or torrent) and the same name breaks a tie, then more seeders.
 */
fun pickSameSource(
    streams: List<Stream>,
    bingeGroup: String?,
    streamName: String?,
    current: StreamInfo? = null,
    wasTorrent: Boolean? = null,
): Stream? {
    if (streams.isEmpty()) return null
    val pool = streams.filter { bingeGroup != null && it.behaviorHints?.bingeGroup == bingeGroup }.ifEmpty { streams }
    val info = pool.associateWith { StreamInfo.of(it) }
    return pool.maxWith(
        compareBy<Stream> { s -> current?.let { StreamInfo.similarity(it, info.getValue(s)) } ?: 0 }
            .thenBy { s -> if (wasTorrent != null && s.isTorrent == wasTorrent) 1 else 0 }
            .thenBy { s -> if (streamName != null && s.name == streamName) 1 else 0 }
            .thenBy { s -> info.getValue(s).seeders ?: 0 }
            .thenByDescending { s -> pool.indexOf(s) },
    )
}
