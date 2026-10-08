package io.github.stivengjekaj.zettastream.iptv

import io.github.stivengjekaj.zettastream.net.Http
import io.github.stivengjekaj.zettastream.source.Source
import io.github.stivengjekaj.zettastream.source.SourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

data class LiveState(
    val channels: List<Channel> = emptyList(),
    val guide: Guide = Guide.Empty,
    val loading: Boolean = false,
    val loaded: Boolean = false,
    val failures: Map<String, String> = emptyMap(),
)

/** Loads the IPTV playlists and the TV guides in the source list. */
class LiveRepository(
    private val http: OkHttpClient,
    private val sources: StateFlow<List<Source>>,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val state = MutableStateFlow(LiveState())
    val live: StateFlow<LiveState> = state.asStateFlow()
    private var job: Job? = null

    init {
        // Load again when the list changes, but only after the first load.
        scope.launch { sources.drop(1).collect { if (state.value.loaded) refresh() } }
    }

    /** Loads the channels the first time that the user opens live TV. */
    fun ensureLoaded() {
        if (!state.value.loaded && job == null) refresh()
    }

    fun refresh() {
        job?.cancel()
        job = scope.launch {
            state.value = state.value.copy(loading = true)
            load(sources.value)
            job = null
        }
    }

    private suspend fun load(all: List<Source>) = coroutineScope {
        val failures = mutableMapOf<String, String>()
        val playlists = all.filter { it.kind == SourceKind.Playlist }.map { source ->
            async { source.url to runCatching { Http.stream(http, source.url) { M3u.parse(it.reader().readText()) } } }
        }.awaitAll()
        playlists.forEach { (url, r) -> r.exceptionOrNull()?.let { failures[url] = it.message ?: "error" } }
        val channels = playlists.mapNotNull { it.second.getOrNull() }.flatMap { it.channels }.distinctBy { it.url }
        state.value = state.value.copy(channels = channels)

        val ids = channels.mapNotNull { it.tvgId }.toSet()
        val guideUrls = (all.filter { it.kind == SourceKind.Guide }.map { it.url } +
            playlists.mapNotNull { it.second.getOrNull() }.flatMap { it.guideUrls }).distinct()
        val now = clock()
        val guides = if (ids.isEmpty()) emptyList() else guideUrls.map { url ->
            async {
                runCatching { Http.stream(http, url) { Xmltv.parse(it, ids, now - 3 * HOUR, now + 24 * HOUR) } }
                    .onFailure { failures[url] = it.message ?: "error" }.getOrNull()
            }
        }.awaitAll().filterNotNull()
        state.value = LiveState(
            channels = channels,
            guide = guides.fold(Guide.Empty) { a, b -> a + b },
            loading = false,
            loaded = true,
            failures = failures,
        )
    }

    private companion object {
        const val HOUR = 3_600_000L
    }
}
