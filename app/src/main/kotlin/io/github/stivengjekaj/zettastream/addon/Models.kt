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
        imdbRating = imdbRating, genres = genres, aliases = aliases,
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
    val externalUrl: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val subtitles: List<Subtitle> = emptyList(),
    val behaviorHints: StreamHints? = null,
) {
    /**
     * The app plays only HTTP streams. A stream with only a torrent hash,
     * a YouTube ID, or an external link is not playable.
     */
    val isPlayable: Boolean
        get() = url != null && (url.startsWith("http://") || url.startsWith("https://"))

    /** The headers that the addon tells the player to send. */
    val requestHeaders: Map<String, String>
        get() = behaviorHints?.proxyHeaders?.request.orEmpty()

    /** The text that tells the user what this stream is. */
    val details: String
        get() = listOfNotNull(title, description, behaviorHints?.filename)
            .firstOrNull { it.isNotBlank() }.orEmpty()
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

    fun preview(m: MetaPreview): MetaPreview = m.copy(name = englishName(m.name, m.aliases))

    fun meta(m: Meta): Meta = m.copy(name = englishName(m.name, m.aliases), videos = m.videos.map(::video))
}
