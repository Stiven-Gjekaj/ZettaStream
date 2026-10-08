package io.github.stivengjekaj.zettastream.ui

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import io.github.stivengjekaj.zettastream.AppContainer
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.addon.Stream
import io.github.stivengjekaj.zettastream.addon.StreamInfo
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
) : Playback

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
    data class Player(val playback: Playback) : Screen
    data object Sources : Screen
    data object KeyTest : Screen

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

    var menuOpen by mutableStateOf(false)
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
        menuOpen = false
        FocusMemory.leave(current)
        if (screen.isTopLevel) {
            stack.clear()
            stack.add(screen)
        } else {
            stack.add(screen)
        }
    }

    /** Replaces the current screen, for example to go to the next episode. */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    /** Goes back one step. See the Back button in docs/decisions.md. */
    fun back(finish: () -> Unit) {
        when {
            menuOpen -> menuOpen = false
            stack.size > 1 -> {
                stack.removeAt(stack.lastIndex)
                FocusMemory.returnTo(current)
            }
            current != Screen.Home -> open(Screen.Home)
            isTv -> exitDialog = true
            else -> finish()
        }
    }

    /** Handles a key that no part of the interface used. */
    fun onUnhandledKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        val action = RemoteKeys.global(event.keyCode) ?: return false
        if (event.repeatCount > 0 && action != RemoteAction.PageDown && action != RemoteAction.PageUp) return true
        screenActions[action]?.let { it(); return true }
        when (action) {
            RemoteAction.ToggleMenu -> menuOpen = !menuOpen
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

/** An item whose left side is nearer than this to the screen edge is at the left edge. */
private const val LEFT_EDGE_DP = 64

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
