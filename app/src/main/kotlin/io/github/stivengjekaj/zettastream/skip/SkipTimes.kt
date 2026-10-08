package io.github.stivengjekaj.zettastream.skip

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap

enum class SkipKind { Opening, Ending, Recap }

/** One part of an episode that the viewer can skip. Times are in milliseconds. */
data class SkipRange(val kind: SkipKind, val start: Long, val end: Long) {
    operator fun contains(position: Long): Boolean = position in start until end
}

/** What the Text button does at one position. */
sealed interface SkipAction {
    data class SeekTo(val position: Long, val kind: SkipKind) : SkipAction
    data class Jump(val by: Long) : SkipAction
}

object Skip {
    /** The jump when no times are known. */
    const val FALLBACK = 85_000L

    /** How long before an opening the button still skips it, for a cold open of a few seconds. */
    private const val LEAD = 3 * 60_000L

    /**
     * Decides what Text does: inside a range, go to its end. Before the
     * opening (in the first minutes), skip the opening. Otherwise jump.
     */
    fun action(ranges: List<SkipRange>, position: Long): SkipAction {
        ranges.firstOrNull { position in it }?.let { return SkipAction.SeekTo(it.end, it.kind) }
        ranges.filter { it.kind == SkipKind.Opening && position < it.start && it.start - position <= LEAD }
            .minByOrNull { it.start }?.let { return SkipAction.SeekTo(it.end, it.kind) }
        return SkipAction.Jump(FALLBACK)
    }

    /** The time before the end where the "Next episode" countdown starts when no ending is known. */
    const val COUNTDOWN_LEAD = 20_000L

    /**
     * Where the "Next episode" countdown starts: at the ending when the times
     * have one, otherwise 20 seconds before the end. Null when the duration
     * is not known yet.
     */
    fun countdownStart(ranges: List<SkipRange>, duration: Long): Long? {
        if (duration <= 0) return null
        val ending = ranges.lastOrNull { it.kind == SkipKind.Ending && it.start < duration }
        return ending?.start ?: (duration - COUNTDOWN_LEAD).coerceAtLeast(0)
    }

    /**
     * The opening or the recap that auto-skip jumps over at this position.
     * A range that was skipped one time is not skipped again, so that the
     * viewer can go back and watch it.
     */
    fun autoSkip(ranges: List<SkipRange>, position: Long, done: Set<Long>): SkipRange? =
        ranges.firstOrNull { it.kind != SkipKind.Ending && position in it && it.start !in done }

    /** The range that the player prompts for at this position, if any. */
    fun current(ranges: List<SkipRange>, position: Long): SkipRange? = ranges.firstOrNull { position in it }

    /** Reads the MyAnimeList ID out of a Kitsu mappings answer. */
    fun malIdFromKitsu(json: String): Int? = runCatching {
        val data = Json.parseToJsonElement(json) as JsonObject
        (data["data"] as JsonArray).firstNotNullOfOrNull { item ->
            val a = (item as JsonObject)["attributes"] as? JsonObject ?: return@firstNotNullOfOrNull null
            val site = (a["externalSite"] as? JsonPrimitive)?.contentOrNull
            if (site == "myanimelist/anime") (a["externalId"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() else null
        }
    }.getOrNull()

    /** Reads an AniSkip answer. Times are seconds in the answer and milliseconds in the result. */
    fun parseAniSkip(json: String): List<SkipRange> = runCatching {
        val root = Json.parseToJsonElement(json) as JsonObject
        if ((root["found"] as? JsonPrimitive)?.booleanOrNull != true) return emptyList()
        val results = (root["results"] as JsonArray).map { it as JsonObject }
        val types = results.mapNotNull { (it["skipType"] as? JsonPrimitive)?.contentOrNull }.toSet()
        // A "mixed" range is a version of the opening or the ending that is mixed into the episode.
        // When the plain range exists too, the two overlap, so the plain range wins.
        results.mapNotNull { o ->
            val kind = when ((o["skipType"] as? JsonPrimitive)?.contentOrNull) {
                "op" -> SkipKind.Opening
                "mixed-op" -> if ("op" in types) return@mapNotNull null else SkipKind.Opening
                "ed" -> SkipKind.Ending
                "mixed-ed" -> if ("ed" in types) return@mapNotNull null else SkipKind.Ending
                "recap" -> SkipKind.Recap
                else -> return@mapNotNull null
            }
            val i = o["interval"] as? JsonObject ?: return@mapNotNull null
            val start = (i["startTime"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            val end = (i["endTime"] as? JsonPrimitive)?.doubleOrNull ?: return@mapNotNull null
            if (end <= start) null else SkipRange(kind, (start * 1000).toLong(), (end * 1000).toLong())
        }.sortedBy { it.start }
    }.getOrDefault(emptyList())

    /** Reads the Kitsu ID and the episode from a video ID such as `kitsu:1376:5`. */
    fun kitsuEpisode(videoId: String): Pair<Int, Int>? {
        val parts = videoId.split(':')
        if (parts.size < 3 || parts[0] != "kitsu") return null
        val id = parts[1].toIntOrNull() ?: return null
        val episode = parts.last().toIntOrNull() ?: return null
        return id to episode
    }
}

/** Gets the opening and ending times of an anime episode from AniSkip. */
class SkipTimes(private val http: OkHttpClient) {
    private val malIds = ConcurrentHashMap<Int, Int>()

    suspend fun forVideo(videoId: String, durationMs: Long): List<SkipRange> = withContext(Dispatchers.IO) {
        val (kitsu, episode) = Skip.kitsuEpisode(videoId) ?: return@withContext emptyList()
        val mal = malIds[kitsu] ?: get("$KITSU/anime/$kitsu/mappings", "application/vnd.api+json")
            ?.let(Skip::malIdFromKitsu)?.also { malIds[kitsu] = it } ?: return@withContext emptyList()
        val seconds = (durationMs / 1000).coerceAtLeast(0)
        val url = "$ANISKIP/skip-times/$mal/$episode?types[]=op&types[]=ed&types[]=recap&types[]=mixed-op&types[]=mixed-ed&episodeLength=$seconds"
        get(url, "application/json")?.let(Skip::parseAniSkip).orEmpty()
    }

    private fun get(url: String, accept: String): String? = runCatching {
        http.newCall(Request.Builder().url(url).header("Accept", accept).build()).execute().use { r ->
            if (r.isSuccessful) r.body.string() else null
        }
    }.getOrNull()

    private companion object {
        const val KITSU = "https://kitsu.app/api/edge"
        const val ANISKIP = "https://api.aniskip.com/v2"
    }
}
