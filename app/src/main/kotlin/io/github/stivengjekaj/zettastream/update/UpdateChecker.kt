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

        /**
         * Tells if [latest] is newer than [current]. Both are `MAJOR.MINOR.PATCH`, with an optional
         * test label such as `-rc1`. A version with a test label comes before the same version without one.
         */
        fun isNewer(latest: String, current: String): Boolean = compare(latest, current) > 0

        fun compare(a: String, b: String): Int {
            val (coreA, testA) = split(a)
            val (coreB, testB) = split(b)
            for (i in 0 until maxOf(coreA.size, coreB.size)) {
                val x = coreA.getOrElse(i) { 0 }
                val y = coreB.getOrElse(i) { 0 }
                if (x != y) return x.compareTo(y)
            }
            return when {
                testA == testB -> 0
                testA == null -> 1
                testB == null -> -1
                else -> compareTest(testA, testB)
            }
        }

        private fun split(version: String): Pair<List<Int>, String?> {
            val text = version.trim().removePrefix("v")
            val core = text.substringBefore('-')
            val test = text.substringAfter('-', "").ifEmpty { null }
            return core.split('.').map { it.toIntOrNull() ?: 0 } to test
        }

        /** Compares two test labels, such as `rc2` and `rc10`, by their letters and then by their number. */
        private fun compareTest(a: String, b: String): Int {
            val letters = a.takeWhile { !it.isDigit() }.compareTo(b.takeWhile { !it.isDigit() })
            if (letters != 0) return letters
            val x = a.filter { it.isDigit() }.toIntOrNull() ?: 0
            val y = b.filter { it.isDigit() }.toIntOrNull() ?: 0
            return x.compareTo(y)
        }
    }
}
