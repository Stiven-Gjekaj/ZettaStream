package io.github.stivengjekaj.zettastream.ui

import io.github.stivengjekaj.zettastream.ui.components.FocusReturn
import io.github.stivengjekaj.zettastream.remote.EdgeGuard
import androidx.compose.ui.focus.FocusRequester
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import io.github.stivengjekaj.zettastream.AppContainer
import io.github.stivengjekaj.zettastream.addon.ListedStream
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.addon.Stream
import io.github.stivengjekaj.zettastream.addon.StreamInfo
import io.github.stivengjekaj.zettastream.addon.StreamOrder
import io.github.stivengjekaj.zettastream.addon.Subtitle
import io.github.stivengjekaj.zettastream.addon.Video
import io.github.stivengjekaj.zettastream.iptv.Channel
import io.github.stivengjekaj.zettastream.remote.RemoteAction
import io.github.stivengjekaj.zettastream.remote.RemoteKeys
import kotlinx.coroutines.launch

/** What the player plays. */
sealed interface Playback

data class VideoPlayback(
    val meta: MetaPreview,
    val videoId: String,
    val label: String,
    val url: String,
    val headers: Map<String, String>,
    val subtitles: List<Subtitle>,
    val episodes: List<Video>,
    /** The addon that gave the stream, so that the next episode can come from the same source. */
    val addonUrl: String? = null,
    val bingeGroup: String? = null,
    val streamName: String? = null,
    /** What the text of the stream said, so that the next episode can match it. */
    val streamInfo: StreamInfo? = null,
    /** A torrent stream: the engine gives the local URL when the player starts. */
    val torrent: TorrentSource? = null,
    /** A short name of the stream, which the player shows at the start. */
    val sourceLabel: String? = null,
    /** The next best streams, in order. The player tries them when this stream fails at the start. */
    val fallbacks: List<ListedStream> = emptyList(),
) : Playback

/** Makes the playback of one stream from the list. */
fun videoPlayback(
    meta: MetaPreview,
    videoId: String,
    label: String,
    episodes: List<Video>,
    listed: ListedStream,
    subtitles: List<Subtitle>,
    fallbacks: List<ListedStream> = emptyList(),
): VideoPlayback = VideoPlayback(
    meta = meta,
    videoId = videoId,
    label = label,
    url = listed.stream.url.orEmpty(),
    headers = listed.stream.requestHeaders,
    subtitles = (listed.stream.subtitles + subtitles).distinctBy { it.url },
    episodes = episodes,
    addonUrl = listed.addon.manifestUrl,
    bingeGroup = listed.stream.behaviorHints?.bingeGroup,
    streamName = listed.stream.name,
    streamInfo = listed.info,
    torrent = listed.stream.torrentSource(),
    sourceLabel = StreamOrder.shortLabel(listed),
    fallbacks = fallbacks,
)

data class TorrentSource(
    val infoHash: String,
    val fileIdx: Int?,
    val sources: List<String>,
    val name: String?,
    val filename: String? = null,
)

fun Stream.torrentSource(): TorrentSource? =
    if (isTorrent) {
        TorrentSource(infoHash!!, fileIdx, sources, behaviorHints?.filename ?: title?.lineSequence()?.firstOrNull(), behaviorHints?.filename)
    } else {
        null
    }

/** Reads the season and the episode from a video ID such as `tt0903747:1:2` or `kitsu:1376:5`. */
fun seasonEpisode(videoId: String): Pair<Int?, Int?> {
    val parts = videoId.split(':')
    return when {
        parts.firstOrNull()?.startsWith("tt") == true && parts.size >= 3 -> parts[1].toIntOrNull() to parts[2].toIntOrNull()
        parts.size >= 3 -> null to parts.last().toIntOrNull()
        else -> null to null
    }
}

data class LivePlayback(val channels: List<Channel>, val index: Int) : Playback

sealed interface Screen {
    data object Home : Screen
    data object Search : Screen
    data object Live : Screen
    data object Library : Screen
    data object Settings : Screen
    data class Detail(val preview: MetaPreview) : Screen
    data class Streams(val meta: MetaPreview, val videoId: String, val label: String, val episodes: List<Video>) : Screen
    /** Finds the best stream of a video, then plays it. Play on a title opens this screen. */
    data class AutoPlay(val meta: MetaPreview, val videoId: String, val label: String, val episodes: List<Video>) : Screen
    data class Player(val playback: Playback) : Screen
    data object Sources : Screen
    data object KeyTest : Screen
    data object HomeRows : Screen

    /**
     * A `when` and not a list: a list in the companion object can hold null
     * for an object that is still in its initialization.
     */
    val isTopLevel: Boolean
        get() = when (this) {
            Home, Search, Live, Library, Settings -> true
            else -> false
        }
}

/**
 * Remembers the focused item of each screen. When the user comes back to a
 * screen, the item with that key takes the focus again.
 */
object FocusMemory {
    private val saved = mutableMapOf<Screen, String>()
    var last: String? = null
    var pending: String? = null

    fun leave(screen: Screen) { last?.let { saved[screen] = it } }

    fun returnTo(screen: Screen) { pending = saved.remove(screen) }
}

/** The state of the interface: the screens, the menu, and the key routing. */
class AppState(val container: AppContainer, val isTv: Boolean) {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()

    /** True while an item of the icon bar of the TV has the focus. */
    var railFocused by mutableStateOf(false)
    /** The item of the icon bar for the screen that is open. Menu and 0 give it the focus. */
    var railEntry: FocusRequester? = null
    /** The screen part next to the icon bar. It gives the focus back to the item that had it. */
    val content = FocusRequester()
    val edgeGuard = EdgeGuard()
    /** True from a change of screen until the new screen has the focus. The icon bar stays closed. */
    var screenChanging by mutableStateOf(false)
    /** The number of key presses. A wait for the focus stops when the viewer presses a key. */
    var keyPresses = 0
    /** True while an item of the screen next to the icon bar has the focus. */
    var contentFocused = false
    /** True after a main screen opens, so that the new screen takes the focus. */
    var focusContentSoon = false
    var exitDialog by mutableStateOf(false)
    var homeFilter by mutableStateOf<String?>(null)
    var focusedMeta by mutableStateOf<MetaPreview?>(null)
    var latestVersion by mutableStateOf<String?>(null)
    var toast by mutableStateOf<String?>(null)

    /** The player and the key test screen take all keys before the rest of the interface. */
    var keyCapture: ((KeyEvent) -> Boolean)? = null

    /** The actions that the current screen gives to the colour and the info buttons. */
    var screenActions: Map<RemoteAction, () -> Unit> = emptyMap()

    var focusManager: FocusManager? = null

    fun open(screen: Screen) {
        FocusMemory.leave(current)
        changeScreen()
        if (screen.isTopLevel) {
            stack.clear()
            stack.add(screen)
        } else {
            stack.add(screen)
        }
    }

    /** Replaces the current screen, for example to go to the next episode. */
    fun replace(screen: Screen) {
        changeScreen()
        stack[stack.lastIndex] = screen
    }

    /**
     * On a TV, the new screen takes the focus. The item that had the focus goes away with the
     * old screen, and the focus must not stop in the icon bar on the way.
     */
    private fun changeScreen() {
        if (!isTv) return
        focusContentSoon = true
        screenChanging = true
    }

    /** Goes back one step. See the Back button in docs/decisions.md. */
    fun back(finish: () -> Unit) {
        when {
            railFocused -> leaveRail()
            stack.size > 1 -> {
                changeScreen()
                stack.removeAt(stack.lastIndex)
                FocusMemory.returnTo(current)
            }
            current != Screen.Home -> open(Screen.Home)
            isTv -> exitDialog = true
            else -> finish()
        }
    }

    /** True while the app itself moves the focus into the icon bar, for Menu or 0. The edge guard does not apply. */
    var railRequested = false

    fun enterRail() {
        railRequested = true
        runCatching { railEntry?.requestFocus() }
        railRequested = false
    }

    /** Gives the focus back to the screen, to the item that had it. */
    fun leaveRail() {
        if (!FocusReturn.restore()) runCatching { content.requestFocus() }
    }

    /** Handles a key that no part of the interface used. */
    fun onUnhandledKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val action = RemoteKeys.global(event.keyCode) ?: return false
        if (event.repeatCount > 0 && action != RemoteAction.PageDown && action != RemoteAction.PageUp) return true
        screenActions[action]?.let { it(); return true }
        when (action) {
            RemoteAction.ToggleMenu -> if (railFocused) leaveRail() else enterRail()
            RemoteAction.Home -> open(Screen.Home)
            RemoteAction.Search -> open(Screen.Search)
            RemoteAction.Library -> open(Screen.Library)
            RemoteAction.Settings -> open(Screen.Settings)
            RemoteAction.LiveTv, RemoteAction.Guide -> open(Screen.Live)
            RemoteAction.PageDown -> focusManager?.moveFocus(FocusDirection.Down)
            RemoteAction.PageUp -> focusManager?.moveFocus(FocusDirection.Up)
            RemoteAction.Info -> focusedMeta?.let { open(Screen.Detail(it)) }
            RemoteAction.Watchlist -> focusedMeta?.let { toggleWatchlist(it) }
            RemoteAction.Filter -> if (current == Screen.Home) cycleFilter()
            else -> return false
        }
        return true
    }

    fun toggleWatchlist(meta: MetaPreview) {
        val adding = !container.library.isInWatchlist(meta.id)
        container.scope.launch { container.library.toggleWatchlist(meta) }
        toast = if (adding) "Added to the watchlist: ${meta.name}" else "Removed from the watchlist: ${meta.name}"
    }

    fun cycleFilter() {
        val options = listOf<String?>(null) + container.addons.types()
        homeFilter = options[(options.indexOf(homeFilter) + 1) % options.size]
        toast = "Filter: ${typeLabel(homeFilter)}"
    }
}

/**
 * One key for types that mean the same thing. Addons write "sport" and
 * "sports", or "tv" and "channel", so the filter shows each kind one time.
 */
fun typeKey(type: String): String = when (val t = type.trim().lowercase()) {
    "sport", "sports", "events" -> "sport"
    "tv", "channel", "channels" -> "tv"
    "movies", "film", "films" -> "movie"
    "show", "shows", "tvshow" -> "series"
    else -> t
}

fun typeLabel(type: String?): String = when (type?.let(::typeKey)) {
    null -> "All"
    "movie" -> "Movies"
    "series" -> "Series"
    "anime" -> "Anime"
    "tv" -> "TV channels"
    "sport" -> "Sports"
    else -> type.replaceFirstChar { it.uppercase() }
}
