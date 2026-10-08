package io.github.stivengjekaj.zettastream.settings

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

/** The choices of the viewer. Each value has a default that works with no change. */
@Serializable
data class ViewerSettings(
    val subtitlesOn: Boolean = false,
    val subtitleLanguage: String = "en",
    val audioLanguage: String = "",
    val subtitleSize: SubtitleSize = SubtitleSize.Normal,
    val autoplayNext: Boolean = true,
    val confirmChannel: Boolean = true,
    val showTorrents: Boolean = true,
    val torrentUpload: Boolean = false,
    val vpnNotice: Boolean = true,
)

/** Keeps the viewer settings in the private storage of the app. */
class SettingsStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val state = MutableStateFlow(read())
    val settings: StateFlow<ViewerSettings> = state.asStateFlow()

    suspend fun update(change: (ViewerSettings) -> ViewerSettings) {
        val next = change(state.value)
        state.value = next
        withContext(Dispatchers.IO) {
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(json.encodeToString(ViewerSettings.serializer(), next))
            temp.renameTo(file)
        }
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
