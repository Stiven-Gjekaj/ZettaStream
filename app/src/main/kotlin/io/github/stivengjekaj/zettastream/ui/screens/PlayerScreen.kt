package io.github.stivengjekaj.zettastream.ui.screens

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import androidx.compose.foundation.border
import io.github.stivengjekaj.zettastream.ui.theme.OnAccent
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import io.github.stivengjekaj.zettastream.skip.SkipRange
import io.github.stivengjekaj.zettastream.skip.SkipKind
import io.github.stivengjekaj.zettastream.skip.SkipAction
import io.github.stivengjekaj.zettastream.skip.Skip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.ui.PlayerView
import io.github.stivengjekaj.zettastream.BuildConfig
import io.github.stivengjekaj.zettastream.addon.Subtitle
import io.github.stivengjekaj.zettastream.addon.StreamInfo
import io.github.stivengjekaj.zettastream.addon.pickSameSource
import io.github.stivengjekaj.zettastream.remote.RemoteAction
import io.github.stivengjekaj.zettastream.remote.RemoteKeys
import io.github.stivengjekaj.zettastream.settings.SubtitleBackground
import io.github.stivengjekaj.zettastream.settings.SubtitleColor
import io.github.stivengjekaj.zettastream.settings.SubtitleEdge
import io.github.stivengjekaj.zettastream.settings.SubtitleFont
import io.github.stivengjekaj.zettastream.settings.SubtitleSize
import io.github.stivengjekaj.zettastream.settings.ViewerSettings
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.LivePlayback
import io.github.stivengjekaj.zettastream.ui.Playback
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.VideoPlayback
import io.github.stivengjekaj.zettastream.ui.seasonEpisode
import io.github.stivengjekaj.zettastream.ui.torrentSource
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

private const val SEEK_STEP = 10_000L
private const val SEEK_FAST = 30_000L
private const val CONFIRM_WINDOW = 3_000L
private const val COUNTDOWN = 10_000L
private const val LOAD_RETRIES = 6

/**
 * Gives each part of the video more tries than the default. When one part of
 * an HLS stream fails two times, the player takes that part from a lower
 * quality for one minute, and does not stop.
 */
@UnstableApi
private object LowerQualityOnError : DefaultLoadErrorHandlingPolicy(LOAD_RETRIES) {
    override fun getFallbackSelectionFor(
        options: LoadErrorHandlingPolicy.FallbackOptions,
        info: LoadErrorHandlingPolicy.LoadErrorInfo,
    ): LoadErrorHandlingPolicy.FallbackSelection? {
        super.getFallbackSelectionFor(options, info)?.let { return it }
        return if (info.errorCount >= 2 && options.isFallbackAvailable(LoadErrorHandlingPolicy.FALLBACK_TYPE_TRACK)) {
            LoadErrorHandlingPolicy.FallbackSelection(LoadErrorHandlingPolicy.FALLBACK_TYPE_TRACK, 60_000)
        } else null
    }
}
/** A key held this long is a long press, also on a remote that does not mark long presses. */
private const val LONG_PRESS_MS = 500L

fun subtitleMime(sub: Subtitle): String {
    val path = sub.url.substringBefore('?').lowercase()
    return when {
        path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        path.endsWith(".ass") || path.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        path.endsWith(".ttml") || path.endsWith(".dfxp") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

/**
 * How much video the player keeps ready. After a stall, the default starts
 * again with 5 seconds ready, so a connection that is only a little slower
 * than the video stops about every 4 seconds. A larger buffer after a stall
 * gives fewer and shorter stops. A torrent gets more, because its speed
 * changes more.
 */
@OptIn(UnstableApi::class)
fun loadControl(isTorrent: Boolean): DefaultLoadControl = DefaultLoadControl.Builder()
    .setBufferDurationsMs(
        /* minBufferMs = */ 30_000,
        /* maxBufferMs = */ if (isTorrent) 120_000 else 90_000,
        /* bufferForPlaybackMs = */ if (isTorrent) 6_000 else 2_500,
        /* bufferForPlaybackAfterRebufferMs = */ if (isTorrent) 20_000 else 12_000,
    )
    .setPrioritizeTimeOverSizeThresholds(true)
    .build()

/** The name of a language code such as "eng" or "sq". An unknown code stays as it is. */
fun languageName(code: String): String {
    if (code.isBlank()) return "Subtitle"
    // Subtitle addons often give three-letter codes such as "sqi", which Java does not read directly.
    val tag = ThreeLetterCodes[code.lowercase()] ?: code
    val locale = Locale.forLanguageTag(tag)
    val name = locale.getDisplayLanguage(Locale.ENGLISH)
    return if (name.isBlank() || name.equals(locale.language, ignoreCase = true)) code else name
}

private val ThreeLetterCodes: Map<String, String> by lazy {
    Locale.getISOLanguages().mapNotNull { two -> runCatching { Locale.forLanguageTag(two).isO3Language to two }.getOrNull() }.toMap()
}

/** Gives each subtitle a readable label. Two tracks in one language become "English 1" and "English 2". */
fun subtitleLabels(subtitles: List<Subtitle>): List<Pair<Subtitle, String>> {
    val names = subtitles.map { languageName(it.lang) }
    val totals = names.groupingBy { it }.eachCount()
    val seen = mutableMapOf<String, Int>()
    return subtitles.zip(names).map { (sub, name) ->
        val n = seen.merge(name, 1, Int::plus)!!
        sub to if (totals.getValue(name) > 1) "$name $n" else name
    }
}

fun formatTime(ms: Long): String {
    if (ms < 0 || ms == C.TIME_UNSET) return "--:--"
    val s = ms / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%d:%02d".format(s / 60, s % 60)
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(app: AppState, playback: Playback) {
    val c = app.container
    val context = LocalContext.current
    val viewer by c.settings.settings.collectAsState()
    val player = remember {
        ExoPlayer.Builder(context).setLoadControl(loadControl(isTorrent = (playback as? VideoPlayback)?.torrent != null)).build().apply {
            playWhenReady = true
            // The viewer settings choose the first subtitle and audio tracks.
            val start = c.settings.settings.value
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !start.subtitlesOn)
                .setPreferredTextLanguage(start.subtitleLanguage.ifEmpty { null })
                .setPreferredAudioLanguage(start.audioLanguage.ifEmpty { null })
                .build()
        }
    }
    var channelIndex by remember { mutableIntStateOf((playback as? LivePlayback)?.index ?: 0) }
    var overlayUntil by remember { mutableLongStateOf(System.currentTimeMillis() + 5000) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var prompt by remember { mutableStateOf<String?>(null) }
    var torrentText by remember { mutableStateOf<String?>(null) }
    var optionsOpen by remember { mutableStateOf(false) }
    var okHeld by remember { mutableStateOf(false) }
    var skips by remember { mutableStateOf(emptyList<SkipRange>()) }
    val autoSkipped = remember { mutableSetOf<Long>() }
    var countdownEnds by remember { mutableStateOf<Long?>(null) }
    var retries by remember { mutableIntStateOf(0) }
    var countdownCancelled by remember { mutableStateOf(false) }
    var countdownLeft by remember { mutableIntStateOf(0) }
    var optionsRow by remember { mutableIntStateOf(0) }
    var tracksVersion by remember { mutableIntStateOf(0) }
    var speed by remember { mutableFloatStateOf(1f) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var pendingStep by remember { mutableIntStateOf(0) }
    var pendingAt by remember { mutableLongStateOf(0L) }
    val live by c.live.live.collectAsState()

    val title: String
    val subtitle: String
    when (playback) {
        is VideoPlayback -> { title = playback.meta.name; subtitle = if (playback.label != playback.meta.name) playback.label else "" }
        is LivePlayback -> {
            val ch = playback.channels[channelIndex]
            title = ch.name
            val (now, next) = live.guide.nowAndNext(ch.tvgId, System.currentTimeMillis())
            subtitle = listOfNotNull(now?.let { "Now: ${it.title}" }, next?.let { "Next: ${it.title}" }).joinToString("   ")
        }
    }

    fun show(text: String? = null) {
        overlayUntil = System.currentTimeMillis() + 4000
        if (text != null) message = text
    }

    fun load(url: String, headers: Map<String, String>, subtitles: List<Subtitle>, startAt: Long) {
        error = null
        val dataSource = OkHttpDataSource.Factory(c.videoHttp).setDefaultRequestProperties(headers)
        val item = MediaItem.Builder()
            .setUri(url)
            .apply { if (url.substringBefore('?').lowercase().endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .setSubtitleConfigurations(subtitleLabels(subtitles).map { (sub, label) ->
                MediaItem.SubtitleConfiguration.Builder(sub.url.toUri())
                    .setMimeType(subtitleMime(sub))
                    .setLanguage(sub.lang.ifBlank { null })
                    .setLabel(label)
                    .build()
            })
            .build()
        // A slow server drops some requests. Each part of the video gets more tries than the default 3.
        val sources = DefaultMediaSourceFactory(dataSource).setLoadErrorHandlingPolicy(LowerQualityOnError)
        player.setMediaSource(sources.createMediaSource(item))
        if (startAt > 0) player.seekTo(startAt)
        player.prepare()
    }

    // Loads the video, or the channel when the user changes it.
    LaunchedEffect(channelIndex) {
        when (playback) {
            is VideoPlayback -> {
                val saved = c.library.progress(playback.videoId)
                val startAt = saved?.takeIf { !it.isFinished }?.position ?: 0
                val torrent = playback.torrent
                if (torrent == null) {
                    load(playback.url, playback.headers, playback.subtitles, startAt)
                } else {
                    // The engine first gets the torrent information from peers, then serves the file locally.
                    torrentText = "Finding peers for the torrent"
                    val (season, episode) = seasonEpisode(playback.videoId)
                    val local = runCatching {
                        c.torrents.open(torrent.infoHash, torrent.sources, torrent.fileIdx, torrent.name, torrent.filename, season, episode)
                    }
                    local.onSuccess {
                        if (BuildConfig.DEBUG) android.util.Log.d("ZPlayer", "torrent url=$it")
                        load(it, emptyMap(), playback.subtitles, startAt)
                    }
                        .onFailure { error = "This torrent does not start (${it.message}). Press Yellow or Back to choose another source." }
                }
            }
            is LivePlayback -> playback.channels[channelIndex].let { load(it.url, it.headers, emptyList(), 0) }
        }
    }

    fun saveProgress() {
        if (playback !is VideoPlayback) return
        val d = player.duration
        if (d == C.TIME_UNSET || d <= 0) return
        val p = player.currentPosition
        c.scope.launch { c.library.saveProgress(playback.videoId, playback.meta, playback.label, p, d) }
    }

    // A stream that played at once has no list under it. Then the list takes the place of the player.
    fun chooseSource() {
        saveProgress()
        val below = app.stack.getOrNull(app.stack.lastIndex - 1)
        if (playback !is VideoPlayback || (below is Screen.Streams && below.videoId == playback.videoId)) app.back {}
        else app.replace(Screen.Streams(playback.meta, playback.videoId, playback.label, playback.episodes))
    }

    LaunchedEffect(Unit) {
        while (true) {
            position = player.currentPosition
            duration = player.duration
            delay(500)
            if (player.isPlaying && System.currentTimeMillis() % 10_000 < 500) saveProgress()
        }
    }

    // While a torrent loads, show its peers and speed.
    LaunchedEffect(Unit) {
        val hash = (playback as? VideoPlayback)?.torrent?.infoHash ?: return@LaunchedEffect
        while (true) {
            withContext(Dispatchers.IO) { c.torrents.state(hash) }?.let { st ->
                torrentText = "Torrent: ${st.peers} peers, %.1f MB/s, %d%% of the file".format(st.downloadRate / 1_000_000f, (st.progress * 100).toInt())
            }
            delay(1000)
        }
    }

    // Get the opening and ending times when the length of the episode is known.
    LaunchedEffect(duration > 0) {
        val video = playback as? VideoPlayback ?: return@LaunchedEffect
        if (duration > 0) skips = c.skipTimes.forVideo(video.videoId, duration)
    }

    val hasNext = (playback as? VideoPlayback)?.let { v ->
        val i = v.episodes.indexOfFirst { it.id == v.videoId }
        i >= 0 && i < v.episodes.lastIndex
    } ?: false

    fun markWatched() {
        val video = playback as? VideoPlayback ?: return
        c.scope.launch { c.library.markWatched(video.videoId) }
    }

    /** Text: go to the end of the opening, ending, or recap. With no times, jump 85 seconds. */
    fun skip() {
        when (val a = Skip.action(skips, player.currentPosition)) {
            is SkipAction.SeekTo -> {
                if (a.kind == SkipKind.Ending) markWatched()
                player.seekTo(a.position)
                show(when (a.kind) { SkipKind.Opening -> "Skipped the opening"; SkipKind.Ending -> "Skipped the ending"; SkipKind.Recap -> "Skipped the recap" })
            }
            is SkipAction.Jump -> { player.seekTo(player.currentPosition + a.by); show("Skipped 85 seconds") }
        }
    }

    fun cycleTrack(type: Int) {
        val options = player.currentTracks.groups.filter { it.type == type }.flatMap { g ->
            (0 until g.length).filter { g.isTrackSupported(it) }.map { g to it }
        }
        if (options.isEmpty()) { show(if (type == C.TRACK_TYPE_TEXT) "No subtitles" else "Only one audio track"); return }
        val textOff = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
        val current = options.indexOfFirst { (g, i) -> g.isTrackSelected(i) }.takeIf { !(type == C.TRACK_TYPE_TEXT && textOff) } ?: -1
        val next = current + 1
        val builder = player.trackSelectionParameters.buildUpon()
        if (type == C.TRACK_TYPE_TEXT && next >= options.size) {
            player.trackSelectionParameters = builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
            show("Subtitles: off")
            return
        }
        val (group, index) = options[next % options.size]
        player.trackSelectionParameters = builder
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
            .build()
        val format = group.getTrackFormat(index)
        val name = format.label ?: format.language?.let { Locale.forLanguageTag(it).displayLanguage } ?: "Track ${next % options.size + 1}"
        show((if (type == C.TRACK_TYPE_TEXT) "Subtitles: " else "Audio: ") + name)
    }

    fun changeEpisode(step: Int) {
        when (playback) {
            is LivePlayback -> {
                channelIndex = (channelIndex + step).mod(playback.channels.size)
                show()
            }
            is VideoPlayback -> {
                val index = playback.episodes.indexOfFirst { it.id == playback.videoId }
                val target = playback.episodes.getOrNull(index + step)
                if (index < 0 || target == null) { show(if (step > 0) "This is the last episode" else "This is the first episode"); return }
                saveProgress()
                val label = episodeLabel(target)
                show("Loading $label")
                val streamsScreen = Screen.Streams(playback.meta, target.id, label, playback.episodes)
                val addonUrl = playback.addonUrl
                if (addonUrl == null) { app.replace(streamsScreen); return }
                // Play the next episode from the same source, with no stop at the list of streams.
                c.scope.launch {
                    val type = playback.meta.type
                    val stream = c.addons.streamsFrom(addonUrl, type, target.id, withTorrents = c.settings.settings.value.showTorrents)
                        ?.let { pickSameSource(it, playback.bingeGroup, playback.streamName, playback.streamInfo, playback.torrent != null) }
                    if (stream == null) { app.replace(streamsScreen); return@launch }
                    val subtitles = withTimeoutOrNull(4000) { c.addons.subtitles(type, target.id) }.orEmpty()
                    app.replace(
                        Screen.Player(
                            playback.copy(
                                videoId = target.id,
                                label = label,
                                url = stream.url.orEmpty(),
                                torrent = stream.torrentSource(),
                                headers = stream.requestHeaders,
                                subtitles = (stream.subtitles + subtitles).distinctBy { it.url },
                                bingeGroup = stream.behaviorHints?.bingeGroup ?: playback.bingeGroup,
                                streamName = stream.name ?: playback.streamName,
                                streamInfo = StreamInfo.of(stream),
                            ),
                        ),
                    )
                }
            }
        }
    }

    /**
     * The first press of a channel button only shows a prompt. A second press of
     * the same button within three seconds changes the episode or the channel.
     */
    fun channelPress(step: Int) {
        if (!viewer.confirmChannel) { changeEpisode(step); return }
        val now = System.currentTimeMillis()
        if (pendingStep == step && now - pendingAt < CONFIRM_WINDOW) {
            pendingStep = 0
            changeEpisode(step)
            return
        }
        pendingStep = step
        pendingAt = now
        val what = when (playback) {
            is LivePlayback -> if (step > 0) "the next channel" else "the previous channel"
            is VideoPlayback -> if (step > 0) "the next episode" else "the previous episode"
        }
        prompt = "Press again for $what"
    }

    // Auto-skip and the "Next episode" countdown follow the position.
    LaunchedEffect(position) {
        if (playback !is VideoPlayback) return@LaunchedEffect
        if (viewer.autoSkipIntro) {
            Skip.autoSkip(skips, position, autoSkipped)?.let { range ->
                autoSkipped += range.start
                player.seekTo(range.end)
                show(if (range.kind == SkipKind.Recap) "Skipped the recap" else "Skipped the opening")
            }
        }
        val start = Skip.countdownStart(skips, duration)
        if (hasNext && viewer.autoplayNext && !countdownCancelled && countdownEnds == null && start != null && position >= start) {
            countdownEnds = System.currentTimeMillis() + COUNTDOWN
        }
        // Going back before the start of the countdown stops it.
        if (countdownEnds != null && start != null && position < start) countdownEnds = null
    }

    LaunchedEffect(countdownEnds) {
        val ends = countdownEnds ?: return@LaunchedEffect
        while (true) {
            val left = ends - System.currentTimeMillis()
            countdownLeft = ((left + 999) / 1000).toInt().coerceAtLeast(0)
            if (left <= 0) break
            delay(250)
        }
        countdownEnds = null
        countdownCancelled = true
        markWatched()
        changeEpisode(1)
    }

    fun optionRows(): List<OptionRow> {
        tracksVersion.let { } // Read, so that the rows change when the tracks change.
        val subs = TrackChoices.choices(player, C.TRACK_TYPE_TEXT)
        val subIndex = TrackChoices.current(player, C.TRACK_TYPE_TEXT, subs)
        val audio = TrackChoices.choices(player, C.TRACK_TYPE_AUDIO)
        val audioIndex = TrackChoices.current(player, C.TRACK_TYPE_AUDIO, audio)
        fun pickSub(step: Int) { TrackChoices.select(player, C.TRACK_TYPE_TEXT, subs[(subIndex + step).mod(subs.size)]); tracksVersion++ }
        fun pickAudio(step: Int) { if (audio.isNotEmpty()) { TrackChoices.select(player, C.TRACK_TYPE_AUDIO, audio[(audioIndex + step).mod(audio.size)]); tracksVersion++ } }
        // A style change goes to the settings. The effect below applies it at once.
        fun <T> cycle(options: List<T>, current: T, step: Int, change: (ViewerSettings, T) -> ViewerSettings) {
            val next = options[(options.indexOf(current) + step).mod(options.size)]
            c.scope.launch { c.settings.update { change(it, next) } }
        }
        fun size(step: Int) = cycle(SubtitleSize.entries, viewer.subtitleSize, step) { v, x -> v.copy(subtitleSize = x) }
        fun setSpeed(step: Int) {
            speed = PlaybackSpeeds[(PlaybackSpeeds.indexOf(speed) + step).coerceIn(0, PlaybackSpeeds.lastIndex)]
            player.setPlaybackSpeed(speed)
        }
        val rows = mutableListOf(
            OptionRow("Subtitles", subs.getOrNull(subIndex)?.label ?: "Off", { pickSub(-1) }, { pickSub(1) }),
            OptionRow("Audio", audio.getOrNull(audioIndex)?.label ?: "One track", { pickAudio(-1) }, { pickAudio(1) }),
            OptionRow("Subtitle size", viewer.subtitleSize.label, { size(-1) }, { size(1) }),
            OptionRow("Text color", viewer.subtitleColor.label,
                { cycle(SubtitleColor.entries, viewer.subtitleColor, -1) { v, x -> v.copy(subtitleColor = x) } },
                { cycle(SubtitleColor.entries, viewer.subtitleColor, 1) { v, x -> v.copy(subtitleColor = x) } }),
            OptionRow("Background", viewer.subtitleBackground.label,
                { cycle(SubtitleBackground.entries, viewer.subtitleBackground, -1) { v, x -> v.copy(subtitleBackground = x) } },
                { cycle(SubtitleBackground.entries, viewer.subtitleBackground, 1) { v, x -> v.copy(subtitleBackground = x) } }),
            OptionRow("Font", viewer.subtitleFont.label,
                { cycle(SubtitleFont.entries, viewer.subtitleFont, -1) { v, x -> v.copy(subtitleFont = x) } },
                { cycle(SubtitleFont.entries, viewer.subtitleFont, 1) { v, x -> v.copy(subtitleFont = x) } }),
            OptionRow("Edge", viewer.subtitleEdge.label,
                { cycle(SubtitleEdge.entries, viewer.subtitleEdge, -1) { v, x -> v.copy(subtitleEdge = x) } },
                { cycle(SubtitleEdge.entries, viewer.subtitleEdge, 1) { v, x -> v.copy(subtitleEdge = x) } }),
        )
        when (playback) {
            is VideoPlayback -> if (playback.episodes.size > 1) {
                rows += OptionRow("Episode", "Previous  /  Next", { optionsOpen = false; changeEpisode(-1) }, { optionsOpen = false; changeEpisode(1) })
            }
            is LivePlayback -> rows += OptionRow("Channel", "Previous  /  Next", { changeEpisode(-1) }, { changeEpisode(1) })
        }
        if (playback is VideoPlayback) {
            rows += OptionRow("Speed", if (speed == 1f) "Normal" else "${speed}x", { setSpeed(-1) }, { setSpeed(1) })
            rows += OptionRow("Source", "Choose another source", onSelect = { chooseSource() })
        }
        return rows
    }

    /** Keys for the options panel while it is open. */
    fun onOptionsKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return true
        // A held OK repeats. Only its first press counts, so that the long press that opened the panel does not select.
        val center = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER
        if (center && event.repeatCount > 0) return true
        val rows = optionRows()
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_0 -> optionsOpen = false
            KeyEvent.KEYCODE_DPAD_UP -> optionsRow = (optionsRow - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_DOWN -> optionsRow = (optionsRow + 1).coerceAtMost(rows.lastIndex)
            KeyEvent.KEYCODE_DPAD_LEFT -> rows.getOrNull(optionsRow)?.onPrevious?.invoke()
            KeyEvent.KEYCODE_DPAD_RIGHT -> rows.getOrNull(optionsRow)?.onNext?.invoke()
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> rows.getOrNull(optionsRow)?.let { r ->
                r.onSelect?.let { optionsOpen = false; it() } ?: r.onNext()
            }
        }
        return true
    }

    fun onKey(event: KeyEvent): Boolean {
        if (optionsOpen) return onOptionsKey(event)
        if (event.action == KeyEvent.ACTION_DOWN && (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_0)) {
            optionsRow = 0
            optionsOpen = true
            return true
        }
        // While the countdown runs, OK plays the next episode now and Back stops the countdown.
        if (countdownEnds != null && event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    // The key comes up later. Mark it as used, so that it does not also pause.
                    okHeld = true
                    countdownEnds = System.currentTimeMillis(); return true
                }
                KeyEvent.KEYCODE_BACK -> { countdownEnds = null; countdownCancelled = true; return true }
            }
        }
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false
        val isOk = event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER ||
            event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
        // OK acts when the key comes up, so that holding it can open the options instead.
        // A remote with no Menu key reaches the options this way.
        if (isOk) {
            when {
                event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 -> okHeld = false
                event.action == KeyEvent.ACTION_DOWN && !okHeld &&
                    (event.isLongPress || event.eventTime - event.downTime >= LONG_PRESS_MS) -> {
                    okHeld = true
                    optionsRow = 0
                    optionsOpen = true
                }
                event.action == KeyEvent.ACTION_UP && !okHeld -> {
                    // While a skip prompt shows, OK skips, as on the large streaming services.
                    if (Skip.current(skips, player.currentPosition) != null) skip()
                    else if (player.isPlaying) player.pause() else player.play()
                    show()
                }
            }
            return true
        }
        if (event.action != KeyEvent.ACTION_DOWN) return true
        val isLive = playback is LivePlayback
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                if (!isLive) player.seekTo((player.currentPosition - if (event.repeatCount > 0) SEEK_FAST else SEEK_STEP).coerceAtLeast(0))
                show(); return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                if (!isLive) player.seekTo(player.currentPosition + if (event.repeatCount > 0) SEEK_FAST else SEEK_STEP)
                show(); return true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> { show(); return true }
        }
        val action = RemoteKeys.player(event.keyCode) ?: return true
        if (event.repeatCount > 0) return true
        when (action) {
            RemoteAction.PlayPause -> if (player.isPlaying) player.pause() else player.play()
            RemoteAction.Play -> player.play()
            RemoteAction.Pause -> player.pause()
            RemoteAction.Next -> { channelPress(1); return true }
            RemoteAction.Previous -> { channelPress(-1); return true }
            RemoteAction.Subtitles -> cycleTrack(C.TRACK_TYPE_TEXT)
            RemoteAction.AudioTrack -> cycleTrack(C.TRACK_TYPE_AUDIO)
            RemoteAction.SkipIntro -> if (!isLive) skip()
            RemoteAction.Info -> show()
            RemoteAction.Sources -> chooseSource()
            else -> RemoteKeys.percentOf(action)?.let { pct ->
                if (!isLive && player.duration > 0) { player.seekTo(player.duration * pct / 100); show("$pct%") }
            }
        }
        show()
        return true
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlayerError(e: PlaybackException) {
                if (BuildConfig.DEBUG) android.util.Log.w("ZPlayer", "error ${e.errorCodeName} at ${player.currentPosition}", e)
                if (playback is VideoPlayback && PlayerErrors.shouldRetry(e.errorCode, retries)) {
                    retries++
                    show("Reconnecting")
                    player.prepare()
                    return
                }
                // The reason from the server or the network, such as "HTTP 403", helps to find the fault.
                val reason = generateSequence<Throwable>(e) { it.cause }.mapNotNull { it.message }.lastOrNull()?.take(80)
                error = PlayerErrors.describe(e.errorCode, e.errorCodeName) + (reason?.let { " ($it)" } ?: "") + " " +
                    if (playback is LivePlayback) "Press Channel up or down for another channel." else "Press Yellow or Back to choose another source."
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (BuildConfig.DEBUG) android.util.Log.d("ZPlayer", "state=$state pos=${player.currentPosition} buf=${player.bufferedPosition} dur=${player.duration}")
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) retries = 0
                if (state == Player.STATE_ENDED && playback is VideoPlayback) {
                    saveProgress()
                    if (c.settings.settings.value.autoplayNext && !countdownCancelled) {
                        countdownEnds = null
                        countdownCancelled = true
                        changeEpisode(1)
                    }
                }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onTracksChanged(tracks: Tracks) { tracksVersion++ }
        }
        player.addListener(listener)
        app.keyCapture = ::onKey
        onDispose {
            app.keyCapture = null
            saveProgress()
            (playback as? VideoPlayback)?.torrent?.let { c.torrents.close(it.infoHash) }
            player.removeListener(listener)
            player.release()
        }
    }

    // Apply the subtitle style when the view exists and each time the viewer changes it.
    LaunchedEffect(viewer, playerView) { SubtitleStyle.apply(playerView?.subtitleView, viewer) }

    LaunchedEffect(message) { if (message != null) { delay(2500); message = null } }
    LaunchedEffect(prompt, pendingAt) { if (prompt != null) { delay(CONFIRM_WINDOW); prompt = null; pendingStep = 0 } }

    val now = System.currentTimeMillis()
    val overlay = app.isTv && (now < overlayUntil || !playing || error != null)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    playerView = this
                    useController = !app.isTv
                    setShowSubtitleButton(true)
                    SubtitleStyle.apply(subtitleView, viewer)
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (buffering && error == null) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Accent)
                torrentText?.let { Text(it, color = TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 16.dp)) }
            }
        }
        if (overlay || (!app.isTv && error != null)) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                    .padding(horizontal = 48.dp, vertical = 32.dp),
            ) {
                Text(title, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                if (subtitle.isNotBlank()) Text(subtitle, color = TextSecondary, fontSize = 16.sp)
                error?.let { Text(it, color = Color(0xFFFF8A8A), fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp)) }
                if (playback is VideoPlayback && duration != C.TIME_UNSET && duration > 0) {
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                        color = Accent, trackColor = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().height(5.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Text(formatTime(position), color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.weight(1f))
                        Text(formatTime(duration), color = TextSecondary, fontSize = 14.sp)
                    }
                }
                if (app.isTv) {
                    Text(
                        if (playback is LivePlayback) "Channel up and down: change channel   Hold OK or Menu: options"
                        else "OK: pause   Left and Right: 10 s   1 to 9: jump   Hold OK or Menu: options",
                        color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
        if (countdownEnds != null) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 140.dp)
                    .clip(Corner).background(Color.Black.copy(alpha = 0.85f)).border(1.dp, Outline, Corner)
                    .clickable { countdownEnds = System.currentTimeMillis() }.padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text("Next episode in $countdownLeft s", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(if (app.isTv) "OK: play now   Back: stay" else "Tap to play now", color = TextSecondary, fontSize = 13.sp)
            }
        } else Skip.current(skips, position)?.let { range ->
            val label = when (range.kind) { SkipKind.Opening -> "Skip intro"; SkipKind.Ending -> "Skip ending"; SkipKind.Recap -> "Skip recap" }
            Text(
                if (app.isTv) "$label: press OK" else label,
                color = OnAccent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 48.dp, bottom = 140.dp)
                    .clip(Corner).background(Accent).clickable { skip() }.padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
        if (optionsOpen) {
            PlayerOptionsPanel(optionRows(), optionsRow, Modifier.align(Alignment.CenterEnd))
        }
        prompt?.let {
            Text(
                it, color = TextSecondary, fontSize = 15.sp,
                modifier = Modifier.align(Alignment.TopStart).padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        message?.let {
            Text(
                it, color = TextPrimary, fontSize = 20.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}
