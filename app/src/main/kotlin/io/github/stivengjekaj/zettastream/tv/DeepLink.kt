package io.github.stivengjekaj.zettastream.tv

import io.github.stivengjekaj.zettastream.addon.MetaPreview
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * A link that opens one title in the app, such as
 * `zettastream://title?id=tt0903747&type=series&name=Breaking%20Bad`.
 * The home screen of the TV opens it from the "Play next" row.
 */
object DeepLink {
    const val SCHEME = "zettastream"

    fun forTitle(meta: MetaPreview): String = "$SCHEME://title?" + listOfNotNull(
        "id" to meta.id, "type" to meta.type, "name" to meta.name, meta.poster?.let { "poster" to it },
    ).joinToString("&") { (k, v) -> "$k=" + URLEncoder.encode(v, "UTF-8") }

    fun parse(link: String): MetaPreview? {
        if (!link.startsWith("$SCHEME://title?")) return null
        val query = link.substringAfter('?').split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=')
            runCatching { key to URLDecoder.decode(pair.substringAfter('=', ""), "UTF-8") }.getOrNull()
        }.toMap()
        val id = query["id"]?.takeIf { it.isNotBlank() } ?: return null
        return MetaPreview(id = id, type = query["type"].orEmpty().ifBlank { "movie" }, name = query["name"].orEmpty(), poster = query["poster"])
    }
}
