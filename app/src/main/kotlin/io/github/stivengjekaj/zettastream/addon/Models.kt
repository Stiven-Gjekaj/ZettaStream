package io.github.stivengjekaj.zettastream.addon

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One title in a catalog. This is the short form of [Meta]. */
@Serializable
data class MetaPreview(
    val id: String,
    val type: String,
    val name: String = "",
    val poster: String? = null,
    val posterShape: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val genres: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    /** The IMDb ID of a title from another database, such as Kitsu. Subtitle addons often know only IMDb IDs. */
    @SerialName("imdb_id") val imdbId: String? = null,
)

/** The full details of one title. */
@Serializable
data class Meta(
    val id: String,
    val type: String,
    val name: String = "",
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val imdbRating: String? = null,
    val runtime: String? = null,
    val genres: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    @SerialName("imdb_id") val imdbId: String? = null,
    val videos: List<Video> = emptyList(),
) {
    fun toPreview() = MetaPreview(
        id = id, type = type, name = name, poster = poster, background = background,
        logo = logo, description = description, releaseInfo = releaseInfo,
        imdbRating = imdbRating, genres = genres, aliases = aliases, imdbId = imdbId,
    )
}

/** One episode of a series, or one part of a title. */
@Serializable
data class Video(
    val id: String,
    val title: String? = null,
    val name: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val released: String? = null,
    val thumbnail: String? = null,
    val overview: String? = null,
    val description: String? = null,
    val imdbSeason: Int? = null,
    val imdbEpisode: Int? = null,
) {
    val label: String get() = title ?: name ?: episode?.let { "Episode $it" } ?: id
    val summary: String? get() = overview ?: description
}

@Serializable
data class Subtitle(
    val id: String? = null,
    val url: String,
    val lang: String = "",
)

@Serializable
data class ProxyHeaders(
    val request: Map<String, String> = emptyMap(),
)

@Serializable
data class StreamHints(
    val notWebReady: Boolean = false,
    val bingeGroup: String? = null,
    val filename: String? = null,
    val proxyHeaders: ProxyHeaders? = null,
)

/** One way to play a video. */
@Serializable
data class Stream(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val sources: List<String> = emptyList(),
    val externalUrl: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val subtitles: List<Subtitle> = emptyList(),
    val behaviorHints: StreamHints? = null,
) {
    /** A stream with an HTTP URL. */
    val isHttp: Boolean
        get() = url != null && (url.startsWith("http://") || url.startsWith("https://"))

    /** A stream with a torrent hash and no HTTP URL. The torrent engine plays it. */
    val isTorrent: Boolean
        get() = !isHttp && infoHash != null && infoHash.length == 40

    /** A YouTube ID or an external link is never playable. */
    val isPlayable: Boolean get() = isHttp

    fun isPlayable(withTorrents: Boolean): Boolean = isHttp || (withTorrents && isTorrent)

    /** The headers that the addon tells the player to send. */
    val requestHeaders: Map<String, String>
        get() = behaviorHints?.proxyHeaders?.request.orEmpty()

    /** All text that tells the user what this stream is: the title, the description, and the file name, each one time. */
    val details: String
        get() {
            val texts = listOfNotNull(title, description, behaviorHints?.filename).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            // A text that is a part of a longer one, such as a file name in the title, shows only once.
            return texts.filter { text -> texts.none { it != text && text in it } }.joinToString("\n")
        }
}

@Serializable
internal data class CatalogResponse(val metas: List<MetaPreview> = emptyList())

@Serializable
internal data class MetaResponse(val meta: Meta? = null)

@Serializable
internal data class StreamResponse(val streams: List<Stream> = emptyList())

@Serializable
internal data class SubtitleResponse(val subtitles: List<Subtitle> = emptyList())

/** Names and numbers that read correctly for a person. */
object Display {
    /**
     * Anime addons give the Japanese name in romaji and the English name as an
     * alias. The first alias in Latin letters is the English name.
     */
    fun englishName(name: String, aliases: List<String>): String =
        aliases.firstOrNull { it.isNotBlank() && isLatin(it) } ?: name

    private fun isLatin(text: String): Boolean {
        val letters = text.filter { it.isLetter() }
        return letters.isNotEmpty() && letters.count { it.code < 0x250 } >= letters.length * 9 / 10
    }

    /**
     * Anime Kitsu gives each season as its own title, with the episodes of
     * season 2 numbered as season 1. The IMDb numbers are correct, so use them.
     */
    fun video(v: Video): Video = v.copy(season = v.imdbSeason ?: v.season, episode = v.imdbEpisode ?: v.episode)

    /**
     * The IMDb form of a video ID, such as `tt2560140:1:1` for `kitsu:7442:1`.
     * Null for an IMDb ID, or when the title has no IMDb numbers.
     */
    fun imdbAlias(meta: MetaPreview, videoId: String, episodes: List<Video>): Pair<String, String>? {
        val imdb = meta.imdbId?.takeIf { it.startsWith("tt") } ?: return null
        if (videoId.startsWith("tt")) return null
        if (episodes.isEmpty() || meta.type == "movie") return "movie" to imdb
        val video = episodes.firstOrNull { it.id == videoId } ?: return null
        val season = video.imdbSeason ?: return null
        val episode = video.imdbEpisode ?: return null
        return "series" to "$imdb:$season:$episode"
    }

    /** The short facts under a card, such as "Movie · 2026". Empty when the addon gives no facts. */
    fun cardFacts(type: String, releaseInfo: String?): String =
        listOfNotNull(kind(type), year(releaseInfo)).joinToString(" · ")

    /** The first year of a release text, such as 2019 for "2019-2023". */
    fun year(releaseInfo: String?): String? = releaseInfo?.let { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }

    /** The rating of a card, such as "7.4". Null when the addon gives no number above 0. */
    fun rating(imdbRating: String?): String? =
        imdbRating?.trim()?.toDoubleOrNull()?.takeIf { it > 0 && it <= 10 }?.let { String.format(java.util.Locale.ROOT, "%.1f", it) }

    private fun kind(type: String): String? = when (type.lowercase()) {
        "movie" -> "Movie"
        "series" -> "Series"
        "anime" -> "Anime"
        "tv", "channel" -> "Channel"
        "" -> null
        else -> type.replaceFirstChar { it.uppercase() }
    }

    fun preview(m: MetaPreview): MetaPreview = m.copy(name = englishName(m.name, m.aliases))

    fun meta(m: Meta): Meta = m.copy(name = englishName(m.name, m.aliases), videos = m.videos.map(::video))
}
