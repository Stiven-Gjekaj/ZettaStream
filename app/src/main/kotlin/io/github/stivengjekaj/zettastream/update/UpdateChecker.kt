package io.github.stivengjekaj.zettastream.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request

/** Asks GitHub for the newest release. It sends no data about the user. */
class UpdateChecker(private val http: OkHttpClient) {
    suspend fun latestVersion(): String? = withContext(Dispatchers.IO) {
        runCatching {
            http.newCall(Request.Builder().url(LATEST_API).header("Accept", "application/vnd.github+json").build())
                .execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val root = Json.parseToJsonElement(response.body.string()) as JsonObject
                    (root["tag_name"] as? JsonPrimitive)?.contentOrNull?.removePrefix("v")
                }
        }.getOrNull()
    }

    companion object {
        const val LATEST_API = "https://api.github.com/repos/Stiven-Gjekaj/ZettaStream/releases/latest"
        const val DOWNLOAD_URL = "https://github.com/Stiven-Gjekaj/ZettaStream/releases/latest/download/ZettaStream.apk"

        /** The AFTVnews short code that opens [DOWNLOAD_URL] in the Downloader app. */
        const val DOWNLOADER_CODE = "8764108"

        /** Tells if [latest] is newer than [current]. Both are `MAJOR.MINOR.PATCH`. */
        fun isNewer(latest: String, current: String): Boolean {
            val a = latest.split('.', '-').map { it.toIntOrNull() ?: 0 }
            val b = current.split('.', '-').map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
