package io.github.stivengjekaj.zettastream.profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/** A picture that a profile can use, with the name of the character or the person. */
data class Avatar(val name: String, val url: String)

/**
 * Gets profile pictures from two free services that need no key: AniList for anime
 * characters, and TVmaze for the characters and the actors of TV shows.
 */
class Avatars(private val http: OkHttpClient) {
    /** The most popular anime characters, and the characters of a few well-known shows. */
    suspend fun featured(): List<Pair<String, List<Avatar>>> = coroutineScope {
        val anime = async { runCatching { anilist(null) }.getOrDefault(emptyList()) }
        val tv = async { FEATURED_SHOWS.map { id -> async { runCatching { Parse.tvmazeCast(get("https://api.tvmaze.com/shows/$id/cast")) }.getOrDefault(emptyList()) } }.flatMap { it.await() } }
        listOf("Anime" to anime.await(), "TV and film" to tv.await()).filter { it.second.isNotEmpty() }
    }

    /** Anime characters and people of TV and film whose name has [query]. */
    suspend fun search(query: String): List<Pair<String, List<Avatar>>> = coroutineScope {
        val q = query.trim()
        val anime = async { runCatching { anilist(q) }.getOrDefault(emptyList()) }
        val people = async {
            runCatching { Parse.tvmazePeople(get("https://api.tvmaze.com/search/people?q=" + URLEncoder.encode(q, "UTF-8"))) }.getOrDefault(emptyList())
        }
        listOf("Anime" to anime.await(), "TV and film" to people.await()).filter { it.second.isNotEmpty() }
    }

    private suspend fun anilist(search: String?): List<Avatar> {
        val query = if (search == null) "{ Page(perPage: 40) { characters(sort: FAVOURITES_DESC) { name { full } image { large } } } }"
        else "query(\$s: String) { Page(perPage: 40) { characters(search: \$s, sort: FAVOURITES_DESC) { name { full } image { large } } } }"
        val body = buildJsonObject {
            put("query", query)
            if (search != null) put("variables", buildJsonObject { put("s", search) })
        }
        return Parse.anilist(post("https://graphql.anilist.co", body.toString()))
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { it.body.string() }
    }

    private suspend fun post(url: String, json: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).post(json.toRequestBody("application/json".toMediaType())).header("Accept", "application/json").build()
        http.newCall(request).execute().use { it.body.string() }
    }

    /** Reads the answers of the two services. A picture without a link is left out. */
    object Parse {
        private val json = Json { ignoreUnknownKeys = true }

        fun anilist(text: String): List<Avatar> {
            val list = path(json.parseToJsonElement(text), "data", "Page", "characters") as? JsonArray ?: return emptyList()
            return list.mapNotNull { c ->
                val name = string(path(c, "name", "full")) ?: return@mapNotNull null
                val url = string(path(c, "image", "large")) ?: return@mapNotNull null
                Avatar(name, url).takeUnless { url.endsWith("/default.jpg") }
            }
        }

        /** The characters of a cast, with the picture of the character, or else of the actor. */
        fun tvmazeCast(text: String): List<Avatar> = (json.parseToJsonElement(text) as? JsonArray).orEmpty().mapNotNull { entry ->
            val character = path(entry, "character")
            val name = string(path(character, "name")) ?: return@mapNotNull null
            val url = string(path(character, "image", "medium")) ?: string(path(entry, "person", "image", "medium")) ?: return@mapNotNull null
            Avatar(name, url)
        }

        fun tvmazePeople(text: String): List<Avatar> = (json.parseToJsonElement(text) as? JsonArray).orEmpty().mapNotNull { entry ->
            val name = string(path(entry, "person", "name")) ?: return@mapNotNull null
            val url = string(path(entry, "person", "image", "medium")) ?: return@mapNotNull null
            Avatar(name, url)
        }

        private fun path(e: JsonElement?, vararg keys: String): JsonElement? = keys.fold(e) { at, key -> (at as? JsonObject)?.get(key) }

        private fun string(e: JsonElement?): String? = (e as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    companion object {
        /** TVmaze IDs: Breaking Bad, Game of Thrones, Stranger Things, The Office, Friends, The Simpsons. */
        val FEATURED_SHOWS = listOf(169, 82, 2993, 526, 431, 83)
    }
}
