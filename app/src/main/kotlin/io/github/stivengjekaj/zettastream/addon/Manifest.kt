package io.github.stivengjekaj.zettastream.addon

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/** One extra argument of a catalog, for example `search` or `genre`. */
data class CatalogExtra(
    val name: String,
    val isRequired: Boolean = false,
    val options: List<String> = emptyList(),
)

data class Catalog(
    val type: String,
    val id: String,
    val name: String,
    val extras: List<CatalogExtra> = emptyList(),
) {
    val supportsSearch: Boolean get() = extras.any { it.name == "search" }

    /** A catalog that needs an argument cannot show on the home screen. */
    val needsExtra: Boolean get() = extras.any { it.isRequired }
}

data class Resource(
    val name: String,
    val types: List<String>? = null,
    val idPrefixes: List<String>? = null,
)

data class Manifest(
    val id: String,
    val name: String,
    val version: String = "",
    val description: String = "",
    val logo: String? = null,
    val types: List<String> = emptyList(),
    val idPrefixes: List<String>? = null,
    val resources: List<Resource> = emptyList(),
    val catalogs: List<Catalog> = emptyList(),
    val configurationRequired: Boolean = false,
) {
    companion object {
        /**
         * Reads a manifest. The protocol permits two forms for a resource and
         * for the extras of a catalog, so this reads the JSON by hand.
         */
        fun parse(root: JsonObject): Manifest = Manifest(
            id = root.string("id") ?: error("The manifest has no id"),
            name = root.string("name") ?: root.string("id").orEmpty(),
            version = root.string("version").orEmpty(),
            description = root.string("description").orEmpty(),
            logo = root.string("logo"),
            types = root.strings("types").orEmpty(),
            idPrefixes = root.strings("idPrefixes"),
            resources = (root["resources"] as? JsonArray).orEmpty().mapNotNull(::parseResource),
            catalogs = (root["catalogs"] as? JsonArray).orEmpty().mapNotNull { parseCatalog(it) },
            configurationRequired = (root["behaviorHints"] as? JsonObject)
                ?.let { (it["configurationRequired"] as? JsonPrimitive)?.booleanOrNull } ?: false,
        )

        private fun parseResource(element: JsonElement): Resource? = when (element) {
            is JsonPrimitive -> element.contentOrNull?.let { Resource(it) }
            is JsonObject -> element.string("name")?.let {
                Resource(it, element.strings("types"), element.strings("idPrefixes"))
            }
            else -> null
        }

        private fun parseCatalog(element: JsonElement): Catalog? {
            val obj = element as? JsonObject ?: return null
            val type = obj.string("type") ?: return null
            val id = obj.string("id") ?: return null
            val extras = (obj["extra"] as? JsonArray)?.mapNotNull { extra ->
                val e = extra as? JsonObject ?: return@mapNotNull null
                CatalogExtra(
                    name = e.string("name") ?: return@mapNotNull null,
                    isRequired = (e["isRequired"] as? JsonPrimitive)?.booleanOrNull ?: false,
                    options = e.strings("options").orEmpty(),
                )
            } ?: run {
                // The old form: two lists of names.
                val required = obj.strings("extraRequired").orEmpty()
                obj.strings("extraSupported").orEmpty().map { CatalogExtra(it, it in required) }
            }
            return Catalog(type, id, obj.string("name") ?: id, extras)
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.contentOrNull

        private fun JsonObject.strings(key: String): List<String>? =
            (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    }
}

/** An installed addon: its manifest and the URL that it answers on. */
data class Addon(
    val manifestUrl: String,
    val manifest: Manifest,
) {
    val baseUrl: String = baseUrlOf(manifestUrl)
    val name: String get() = manifest.name

    /** Tells if this addon gives [resource] for a title of [type] with [id]. */
    fun supports(resource: String, type: String, id: String): Boolean {
        val entry = manifest.resources.firstOrNull { it.name == resource } ?: return false
        val types = entry.types ?: manifest.types
        if (type !in types) return false
        val prefixes = entry.idPrefixes ?: manifest.idPrefixes
        return prefixes == null || prefixes.any { id.startsWith(it) }
    }

    /** Makes the URL of one request, in the form that the protocol defines. */
    fun resourceUrl(
        resource: String,
        type: String,
        id: String,
        extras: Map<String, String> = emptyMap(),
    ): String {
        val extra = if (extras.isEmpty()) "" else
            "/" + extras.entries.joinToString("&") { "${it.key}=${encodeComponent(it.value)}" }
        return "$baseUrl/$resource/${encodeComponent(type)}/${encodeComponent(id)}$extra.json"
    }

    companion object {
        /** Changes a `stremio://` link to HTTPS and removes `/manifest.json`. */
        fun normalizeManifestUrl(url: String): String {
            val trimmed = url.trim()
            return if (trimmed.startsWith("stremio://")) "https://" + trimmed.removePrefix("stremio://") else trimmed
        }

        fun baseUrlOf(manifestUrl: String): String =
            normalizeManifestUrl(manifestUrl).substringBefore('?').removeSuffix("/manifest.json").trimEnd('/')

        /** Encodes like `encodeURIComponent` in JavaScript, as Stremio does. */
        fun encodeComponent(value: String): String {
            val bytes = value.toByteArray(Charsets.UTF_8)
            val out = StringBuilder()
            for (b in bytes) {
                val c = b.toInt() and 0xff
                val ch = c.toChar()
                if (ch.isLetterOrDigit() && c < 128 || ch in "-_.!~*'()") out.append(ch)
                else out.append('%').append("%02X".format(c))
            }
            return out.toString()
        }
    }
}

private fun JsonArray?.orEmpty(): List<JsonElement> = this ?: emptyList()
