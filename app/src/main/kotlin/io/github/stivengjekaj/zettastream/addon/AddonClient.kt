package io.github.stivengjekaj.zettastream.addon

import kotlinx.coroutines.suspendCancellableCoroutine
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
        val root = json.parseToJsonElement(get(url)) as? JsonObject
            ?: throw IOException("The manifest is not a JSON object")
        return Addon(url, Manifest.parse(root))
    }

    suspend fun catalog(addon: Addon, catalog: Catalog, extras: Map<String, String> = emptyMap()): List<MetaPreview> =
        json.decodeFromString<CatalogResponse>(get(addon.resourceUrl("catalog", catalog.type, catalog.id, extras))).metas
            .filter { it.id.isNotBlank() }
            .map(Display::preview)

    suspend fun meta(addon: Addon, type: String, id: String): Meta? =
        json.decodeFromString<MetaResponse>(get(addon.resourceUrl("meta", type, id))).meta?.let(Display::meta)

    suspend fun streams(addon: Addon, type: String, videoId: String): List<Stream> =
        json.decodeFromString<StreamResponse>(get(addon.resourceUrl("stream", type, videoId))).streams

    suspend fun subtitles(addon: Addon, type: String, videoId: String): List<Subtitle> =
        json.decodeFromString<SubtitleResponse>(get(addon.resourceUrl("subtitles", type, videoId))).subtitles

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
