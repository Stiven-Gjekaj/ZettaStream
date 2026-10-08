package io.github.stivengjekaj.zettastream.addon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

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

    suspend fun meta(addon: Addon, type: String, id: String): Meta? =
        json.decodeFromString<MetaResponse>(get(addon.resourceUrl("meta", type, id))).meta

    suspend fun streams(addon: Addon, type: String, videoId: String): List<Stream> =
        json.decodeFromString<StreamResponse>(get(addon.resourceUrl("stream", type, videoId))).streams

    suspend fun subtitles(addon: Addon, type: String, videoId: String): List<Subtitle> =
        json.decodeFromString<SubtitleResponse>(get(addon.resourceUrl("subtitles", type, videoId))).subtitles

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code} from $url")
            response.body.string()
        }
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
