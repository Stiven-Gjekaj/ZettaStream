package io.github.stivengjekaj.zettastream.iptv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** One live channel from a playlist. */
data class Channel(
    val name: String,
    val url: String,
    val tvgId: String? = null,
    val logo: String? = null,
    val group: String? = null,
    val headers: Map<String, String> = emptyMap(),
) {
    /** A stable key for the channel, also when the playlist changes order. */
    val key: String get() = tvgId?.takeIf { it.isNotBlank() } ?: url
}

data class Playlist(
    val channels: List<Channel>,
    val guideUrls: List<String>,
)

/** Reads an M3U playlist in the extended IPTV form. */
object M3u {
    private val attribute = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): Playlist {
        val channels = mutableListOf<Channel>()
        val guides = mutableListOf<String>()
        var info: Map<String, String> = emptyMap()
        var name: String? = null
        var headers = mutableMapOf<String, String>()

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith("#EXTM3U") -> {
                    val attrs = attributes(line)
                    (attrs["x-tvg-url"] ?: attrs["url-tvg"])?.split(',')
                        ?.map { it.trim() }?.filter { it.isNotEmpty() }?.let(guides::addAll)
                }
                line.startsWith("#EXTINF") -> {
                    val split = nameComma(line)
                    info = attributes(if (split < 0) line else line.substring(0, split))
                    name = if (split < 0) null else line.substring(split + 1).trim().ifEmpty { null }
                }
                line.startsWith("#EXTVLCOPT:") -> {
                    val option = line.removePrefix("#EXTVLCOPT:")
                    val key = option.substringBefore('=').trim().lowercase()
                    val value = option.substringAfter('=', "").trim()
                    when (key) {
                        "http-user-agent" -> headers["User-Agent"] = value
                        "http-referrer", "http-referer" -> headers["Referer"] = value
                        "http-origin" -> headers["Origin"] = value
                    }
                }
                line.startsWith("#EXTHTTP:") -> {
                    runCatching { Json.parseToJsonElement(line.removePrefix("#EXTHTTP:")) as JsonObject }
                        .getOrNull()?.forEach { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { headers[k] = it } }
                }
                line.startsWith("#") -> Unit
                else -> {
                    val (url, pipeHeaders) = splitPipeHeaders(line)
                    channels += Channel(
                        name = name ?: info["tvg-name"] ?: url,
                        url = url,
                        tvgId = info["tvg-id"]?.ifBlank { null },
                        logo = info["tvg-logo"]?.ifBlank { null },
                        group = info["group-title"]?.ifBlank { null },
                        headers = headers + pipeHeaders,
                    )
                    info = emptyMap()
                    name = null
                    headers = mutableMapOf()
                }
            }
        }
        return Playlist(channels, guides)
    }

    private fun attributes(line: String): Map<String, String> =
        attribute.findAll(line).associate { it.groupValues[1].lowercase() to it.groupValues[2] }

    /** Finds the comma before the name. A comma inside quotes does not count. */
    private fun nameComma(line: String): Int {
        var quoted = false
        line.forEachIndexed { i, c ->
            if (c == '"') quoted = !quoted
            else if (c == ',' && !quoted) return i
        }
        return -1
    }

    /** Reads the Kodi form `url|User-Agent=x&Referer=y`. */
    private fun splitPipeHeaders(line: String): Pair<String, Map<String, String>> {
        val index = line.indexOf('|')
        if (index < 0) return line to emptyMap()
        val headers = line.substring(index + 1).split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=', "").trim()
            if (key.isEmpty()) null else key to java.net.URLDecoder.decode(pair.substringAfter('='), "UTF-8")
        }.toMap()
        return line.substring(0, index) to headers
    }
}
