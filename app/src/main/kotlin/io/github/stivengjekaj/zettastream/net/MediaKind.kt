package io.github.stivengjekaj.zettastream.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder

/** The kind of a stream link, which tells the player how to read it. */
enum class MediaKind { Hls, File }

object MediaKinds {
    private val BYTE_ORDER_MARK = Char(0xFEFF)
    private val fileEndings = listOf(".mp4", ".mkv", ".webm", ".avi", ".mov", ".ts", ".m4v", ".mp3", ".aac")

    /**
     * Finds the kind from the link. An HLS link from a proxy often ends in a
     * name such as `/proxy/bound`, with the `.m3u8` address in a parameter.
     * Null when the link does not tell.
     */
    fun fromUrl(url: String): MediaKind? {
        val text = runCatching { URLDecoder.decode(url, "UTF-8") }.getOrDefault(url).lowercase()
        val path = text.substringBefore('?').substringBefore('#')
        return when {
            path.endsWith(".m3u8") || path.endsWith(".m3u") -> MediaKind.Hls
            fileEndings.any { path.endsWith(it) } -> MediaKind.File
            ".m3u8" in text -> MediaKind.Hls
            else -> null
        }
    }

    /** Finds the kind from the answer of the server: its content type and its first bytes. */
    fun fromAnswer(contentType: String?, start: ByteArray): MediaKind? {
        val type = contentType.orEmpty().lowercase()
        val head = String(start, Charsets.UTF_8).trimStart(BYTE_ORDER_MARK, ' ', '\r', '\n', '\t')
        return when {
            "mpegurl" in type || head.startsWith("#EXTM3U") -> MediaKind.Hls
            type.startsWith("video/") || type.startsWith("audio/") || "octet-stream" in type -> MediaKind.File
            else -> null
        }
    }

    /**
     * Finds the kind from the link, or else asks the server for the first bytes.
     * When neither tells, the player reads the link as a file.
     */
    suspend fun detect(http: OkHttpClient, url: String, headers: Map<String, String>): MediaKind =
        fromUrl(url) ?: withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }
                    .header("Range", "bytes=0-1023").build()
                http.newCall(request).execute().use { response ->
                    val start = response.body.source().let { source -> source.request(1024); source.buffer.readByteArray(minOf(1024L, source.buffer.size)) }
                    fromAnswer(response.header("Content-Type"), start)
                }
            }.getOrNull()
        } ?: MediaKind.File
}
