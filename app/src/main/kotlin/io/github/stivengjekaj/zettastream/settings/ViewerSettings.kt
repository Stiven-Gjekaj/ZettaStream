package io.github.stivengjekaj.zettastream.settings

import io.github.stivengjekaj.zettastream.storage.SafeFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

enum class SubtitleSize(val label: String, val scale: Float) {
    Small("Small", 0.8f), Normal("Normal", 1f), Large("Large", 1.3f), Huge("Huge", 1.6f)
}

/** The text color of subtitles, as ARGB. */
enum class SubtitleColor(val label: String, val argb: Int) {
    White("White", 0xFFFFFFFF.toInt()), Yellow("Yellow", 0xFFFFE14D.toInt()),
    Gray("Light gray", 0xFFD0D0D0.toInt()), Cyan("Cyan", 0xFF7FE7FF.toInt()),
}

/** How much the box behind subtitles hides the video. */
enum class SubtitleBackground(val label: String, val alpha: Int) {
    None("None", 0), Light("25%", 64), Medium("50%", 128), Dark("75%", 191), Solid("Solid", 255)
}

enum class SubtitleFont(val label: String) { Default("Default"), Sans("Sans"), Serif("Serif"), Mono("Monospace"), Bold("Bold") }

/** The edge around each letter, so that text without a box stays readable. */
enum class SubtitleEdge(val label: String) { None("None"), Outline("Outline"), Shadow("Drop shadow"), Raised("Raised") }

/** The choices of the viewer. Each value has a default that works with no change. */
@Serializable
data class ViewerSettings(
    val subtitlesOn: Boolean = false,
    val subtitleLanguage: String = "en",
    val audioLanguage: String = "",
    val subtitleSize: SubtitleSize = SubtitleSize.Normal,
    val subtitleColor: SubtitleColor = SubtitleColor.White,
    val subtitleBackground: SubtitleBackground = SubtitleBackground.None,
    val subtitleFont: SubtitleFont = SubtitleFont.Default,
    val subtitleEdge: SubtitleEdge = SubtitleEdge.Outline,
    val autoplayNext: Boolean = true,
    val autoSkipIntro: Boolean = false,
    val confirmChannel: Boolean = true,
    val showTorrents: Boolean = true,
    val directFirst: Boolean = false,
    val torrentUpload: Boolean = false,
    val vpnNotice: Boolean = true,
    /** The keys of the home rows that the viewer turned off. */
    val hiddenRows: Set<String> = emptySet(),
)

/** Keeps the viewer settings in the private storage of the app. */
class SettingsStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val writer = SafeFile(file)
    private val state = MutableStateFlow(read())
    val settings: StateFlow<ViewerSettings> = state.asStateFlow()

    suspend fun update(change: (ViewerSettings) -> ViewerSettings) {
        val next = change(state.value)
        state.value = next
        writer.write(json.encodeToString(ViewerSettings.serializer(), next))
    }

    private fun read(): ViewerSettings = runCatching {
        json.decodeFromString(ViewerSettings.serializer(), file.readText())
    }.getOrDefault(ViewerSettings())

    companion object {
        /** The languages that the settings offer, as ISO 639-1 codes. An empty code means the original. */
        val Languages = listOf(
            "" to "Original", "en" to "English", "sq" to "Albanian", "ja" to "Japanese", "it" to "Italian",
            "de" to "German", "fr" to "French", "es" to "Spanish", "pt" to "Portuguese", "tr" to "Turkish",
            "ar" to "Arabic", "ru" to "Russian", "ko" to "Korean", "zh" to "Chinese",
        )

        fun languageName(code: String): String = Languages.firstOrNull { it.first == code }?.second ?: code

        /** Gives the next item of [options] after [current], and the first after the last. */
        fun <T> next(options: List<T>, current: T): T = options[(options.indexOf(current) + 1) % options.size]
    }
}
