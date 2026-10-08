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
) : Playback

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
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            !isTv -> if (current != Screen.Home) open(Screen.Home) else finish()
            menuOpen -> exitDialog = true
            else -> menuOpen = true
        }
    }

    /**
     * Opens the menu when the user presses Left on an item at the left edge.
     * Without this, Left on the first poster of a row moves the focus up.
     */
    fun opensMenuOnLeft(event: KeyEvent, focusedLeft: Float, density: Float): Boolean {
        if (!isTv || menuOpen || current is Screen.Player || current is Screen.KeyTest) return false
        if (event.keyCode != KeyEvent.KEYCODE_DPAD_LEFT || event.action != KeyEvent.ACTION_DOWN) return false
        if (focusedLeft > LEFT_EDGE_DP * density) return false
        menuOpen = true
        return true
    }

    /** Handles a key that no part of the interface used. */
    fun onUnhandledKey(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && isTv && current !is Screen.Player && !menuOpen) {
            menuOpen = true
            return true
        }
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

fun typeLabel(type: String?): String = when (type) {
    null -> "All"
    "movie" -> "Movies"
    "series" -> "Series"
    "anime" -> "Anime"
    "tv", "channel" -> "TV channels"
    else -> type.replaceFirstChar { it.uppercase() }
}
