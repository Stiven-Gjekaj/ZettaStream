package io.github.stivengjekaj.zettastream.addon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Sends the requests of the Stremio addon protocol. */
class AddonClient(
    private val http: OkHttpClient,
    private val json: Json = DefaultJson,
) {
    suspend fun install(manifestUrl: String): Addon {
        val url = Addon.normalizeManifestUrl(manifestUrl)
        val text = get(url)
        val root = decode { json.parseToJsonElement(text) } as? JsonObject
            ?: throw IOException("The manifest is not a JSON object")
        return Addon(url, decode { Manifest.parse(root) })
    }

    /**
     * Reads an answer away from the main thread. A search asks many addons at the same time,
     * and their answers read on the main thread made the screen stop.
     */
    private suspend fun <T> decode(block: () -> T): T = withContext(Dispatchers.Default) { block() }

    suspend fun catalog(addon: Addon, catalog: Catalog, extras: Map<String, String> = emptyMap()): List<MetaPreview> {
        val text = get(addon.resourceUrl("catalog", catalog.type, catalog.id, extras))
        return decode { json.decodeFromString<CatalogResponse>(text).metas.filter { it.id.isNotBlank() }.map(Display::preview) }
    }

    suspend fun meta(addon: Addon, type: String, id: String): Meta? {
        val text = get(addon.resourceUrl("meta", type, id))
        return decode { json.decodeFromString<MetaResponse>(text).meta?.let(Display::meta) }
    }

    suspend fun streams(addon: Addon, type: String, videoId: String): List<Stream> {
        val text = get(addon.resourceUrl("stream", type, videoId))
        return decode { json.decodeFromString<StreamResponse>(text).streams }
    }

    suspend fun subtitles(addon: Addon, type: String, videoId: String): List<Subtitle> {
        val text = get(addon.resourceUrl("subtitles", type, videoId))
        return decode { json.decodeFromString<SubtitleResponse>(text).subtitles }
    }

    /** Gets [url]. When the coroutine stops, for example after a time limit, the request stops too. */
    private suspend fun get(url: String): String = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(Request.Builder().url(url).build())
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) = continuation.resumeWithException(e)
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (!it.isSuccessful) throw IOException("HTTP ${it.code} from $url")
                        it.body.string()
                    }
                }
                result.fold({ continuation.resume(it) }, { continuation.resumeWithException(it) })
            }
        })
    }

    companion object {
        val DefaultJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}
