package io.github.stivengjekaj.zettastream.ui.screens

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import io.github.stivengjekaj.zettastream.addon.Subtitle
import io.github.stivengjekaj.zettastream.addon.pickSameSource
import io.github.stivengjekaj.zettastream.remote.RemoteAction
import io.github.stivengjekaj.zettastream.remote.RemoteKeys
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.LivePlayback
import io.github.stivengjekaj.zettastream.ui.Playback
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.VideoPlayback
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

private const val SEEK_STEP = 10_000L
private const val SEEK_FAST = 30_000L
private const val INTRO_SKIP = 85_000L
private const val CONFIRM_WINDOW = 3_000L

fun subtitleMime(sub: Subtitle): String {
    val path = sub.url.substringBefore('?').lowercase()
    return when {
        path.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        path.endsWith(".ass") || path.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        path.endsWith(".ttml") || path.endsWith(".dfxp") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
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
    val player = remember { ExoPlayer.Builder(context).build().apply { playWhenReady = true } }
    var channelIndex by remember { mutableIntStateOf((playback as? LivePlayback)?.index ?: 0) }
    var overlayUntil by remember { mutableLongStateOf(System.currentTimeMillis() + 5000) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var buffering by remember { mutableStateOf(true) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(C.TIME_UNSET) }
    var prompt by remember { mutableStateOf<String?>(null) }
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
        val dataSource = OkHttpDataSource.Factory(c.http).setDefaultRequestProperties(headers)
        val item = MediaItem.Builder()
            .setUri(url)
            .apply { if (url.substringBefore('?').lowercase().endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .setSubtitleConfigurations(subtitles.map { sub ->
                MediaItem.SubtitleConfiguration.Builder(sub.url.toUri())
                    .setMimeType(subtitleMime(sub))
                    .setLanguage(sub.lang.ifBlank { null })
                    .setLabel(sub.lang.ifBlank { "Subtitle" })
                    .build()
            })
            .build()
        player.setMediaSource(DefaultMediaSourceFactory(dataSource).createMediaSource(item))
        if (startAt > 0) player.seekTo(startAt)
        player.prepare()
    }

    // Loads the video, or the channel when the user changes it.
    LaunchedEffect(channelIndex) {
        when (playback) {
            is VideoPlayback -> {
                val saved = c.library.progress(playback.videoId)
                load(playback.url, playback.headers, playback.subtitles, saved?.takeIf { !it.isFinished }?.position ?: 0)
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

    LaunchedEffect(Unit) {
        while (true) {
            position = player.currentPosition
            duration = player.duration
            delay(500)
            if (player.isPlaying && System.currentTimeMillis() % 10_000 < 500) saveProgress()
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
                    val stream = c.addons.streamsFrom(addonUrl, type, target.id)
                        ?.let { pickSameSource(it, playback.bingeGroup, playback.streamName) }
                    if (stream == null) { app.replace(streamsScreen); return@launch }
                    val subtitles = withTimeoutOrNull(4000) { c.addons.subtitles(type, target.id) }.orEmpty()
                    app.replace(
                        Screen.Player(
                            playback.copy(
                                videoId = target.id,
                                label = label,
                                url = stream.url!!,
                                headers = stream.requestHeaders,
                                subtitles = (stream.subtitles + subtitles).distinctBy { it.url },
                                bingeGroup = stream.behaviorHints?.bingeGroup ?: playback.bingeGroup,
                                streamName = stream.name ?: playback.streamName,
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

    fun onKey(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true
        val isLive = playback is LivePlayback
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.repeatCount == 0) { if (player.isPlaying) player.pause() else player.play() }
                show(); return true
            }
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
            RemoteAction.SkipIntro -> if (!isLive) { player.seekTo(player.currentPosition + INTRO_SKIP); show("Skipped 85 seconds") }
            RemoteAction.Info -> show()
            RemoteAction.Sources -> { saveProgress(); app.back {} }
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
                error = "This stream does not play (${e.errorCodeName}). " +
                    if (playback is LivePlayback) "Press Channel up or down for another channel." else "Press Yellow or Back to choose another source."
            }
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED && playback is VideoPlayback) { saveProgress(); changeEpisode(1) }
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onTracksChanged(tracks: Tracks) {}
        }
        player.addListener(listener)
        app.keyCapture = ::onKey
        onDispose {
            app.keyCapture = null
            saveProgress()
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(message) { if (message != null) { delay(2500); message = null } }
    LaunchedEffect(prompt, pendingAt) { if (prompt != null) { delay(CONFIRM_WINDOW); prompt = null; pendingStep = 0 } }

    val now = System.currentTimeMillis()
    val overlay = app.isTv && (now < overlayUntil || !playing || error != null)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = !app.isTv
                    setShowSubtitleButton(true)
                    keepScreenOn = true
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (buffering && error == null) CircularProgressIndicator(color = Accent, modifier = Modifier.align(Alignment.Center))
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
                        if (playback is LivePlayback) "Channel up and down: change channel   Language: audio   Subtitles: subtitles"
                        else "OK: pause   Left and Right: 10 s   1 to 9: jump   Channel: episode   Text: skip intro",
                        color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
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
