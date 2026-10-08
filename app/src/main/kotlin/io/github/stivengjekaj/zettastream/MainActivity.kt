package io.github.stivengjekaj.zettastream

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.PhoneTabBar
import io.github.stivengjekaj.zettastream.ui.components.Toast
import io.github.stivengjekaj.zettastream.ui.components.TvSideMenu
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.screens.DetailScreen
import io.github.stivengjekaj.zettastream.ui.screens.HomeScreen
import io.github.stivengjekaj.zettastream.ui.screens.KeyTestScreen
import io.github.stivengjekaj.zettastream.ui.screens.LibraryScreen
import io.github.stivengjekaj.zettastream.ui.screens.LiveScreen
import io.github.stivengjekaj.zettastream.ui.screens.PlayerScreen
import io.github.stivengjekaj.zettastream.ui.screens.SearchScreen
import io.github.stivengjekaj.zettastream.ui.screens.SettingsScreen
import io.github.stivengjekaj.zettastream.ui.screens.SourcesScreen
import io.github.stivengjekaj.zettastream.ui.screens.StreamsScreen
import io.github.stivengjekaj.zettastream.ui.theme.Background
import io.github.stivengjekaj.zettastream.ui.theme.ZettaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var app: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        app = AppState((application as ZettaStreamApp).container, deviceKind() == DeviceKind.Tv)
        setContent {
            ZettaTheme {
                // On a TV, a control is only as large as it looks, so the focus ring fits it.
                // A phone keeps the larger touch area.
                val minimum = if (app.isTv) Dp.Unspecified else LocalMinimumInteractiveComponentSize.current
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides minimum) {
                    Root(app) { finish() }
                }
            }
        }
    }

    /** A start with no network leaves addons that did not answer. Try them again on return. */
    override fun onStart() {
        super.onStart()
        if (app.container.addons.addons.value.failures.isNotEmpty()) app.container.addons.retry()
    }

    /**
     * The player and the key test take keys first. A key that nobody used goes to the app actions.
     * Lint marks this override as a restricted API by mistake: the method is public in Activity.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        app.keyCapture?.let { if (it(event)) return true }
        if (super.dispatchKeyEvent(event)) return true
        return app.onUnhandledKey(event)
    }
}

@Composable
private fun Root(app: AppState, finish: () -> Unit) {
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        app.focusManager = focusManager
        app.latestVersion = app.container.updates.latestVersion()
    }
    LaunchedEffect(app.toast) { if (app.toast != null) { delay(2500); app.toast = null } }
    BackHandler { app.back(finish) }

    val screen = app.current
    val states = rememberSaveableStateHolder()
    val fullScreen = screen is Screen.Player
    Box(Modifier.fillMaxSize().background(Background)) {
        Column(Modifier.fillMaxSize().then(if (fullScreen || app.isTv) Modifier else Modifier.windowInsetsPadding(WindowInsets.safeDrawing))) {
            // A new key for each screen, so that a player for the next episode starts fresh.
            // Each screen keeps its scroll positions while it is on the stack, so Back returns to the same place.
            Box(Modifier.weight(1f)) {
                key(screen) { states.SaveableStateProvider(screen.toString()) { Content(app, screen) } }
            }
            if (!app.isTv && screen.isTopLevel) PhoneTabBar(screen) { app.open(it) }
        }
        if (app.isTv && !fullScreen) {
            TvSideMenu(app.menuOpen, screen, onSelect = { app.open(it) })
        }
        Toast(app.toast, Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
    }

    VpnNotice(app)

    if (app.exitDialog) {
        val stay = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { app.exitDialog = false },
            title = { Text("Close ZettaStream?") },
            confirmButton = { TextButton(shape = Corner, onClick = finish, modifier = Modifier.focusRing()) { Text("Close") } },
            dismissButton = {
                TextButton(shape = Corner, onClick = { app.exitDialog = false }, modifier = Modifier.focusRequester(stay).focusRing()) { Text("Stay") }
            },
        )
        LaunchedEffect(Unit) { runCatching { stay.requestFocus() } }
    }
}

@Composable
private fun Content(app: AppState, screen: Screen) {
    when (screen) {
        Screen.Home -> HomeScreen(app)
        Screen.Search -> SearchScreen(app)
        Screen.Live -> LiveScreen(app)
        Screen.Library -> LibraryScreen(app)
        Screen.Settings -> SettingsScreen(app)
        Screen.Sources -> SourcesScreen(app)
        Screen.KeyTest -> KeyTestScreen(app)
        is Screen.Detail -> DetailScreen(app, screen.preview)
        is Screen.Streams -> StreamsScreen(app, screen)
        is Screen.Player -> PlayerScreen(app, screen.playback)
    }
}

/**
 * Asks the user to use a VPN at each start, until they turn the notice off.
 * The app does not check for a VPN and does not stop without one.
 */
@Composable
private fun VpnNotice(app: AppState) {
    val settings by app.container.settings.settings.collectAsState()
    var shown by rememberSaveable { mutableStateOf(false) }
    if (shown || !settings.vpnNotice) return
    val ok = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = { shown = true },
        title = { Text("Use a VPN") },
        text = {
            Text(
                "Torrent streams connect your device to other people. They can see your IP address, " +
                    "and in many countries a copyright holder can act against it. Use a VPN that permits P2P. " +
                    "ZettaStream does not check this. You can turn off torrent streams, or this notice, in Settings.",
            )
        },
        confirmButton = {
            TextButton(shape = Corner, onClick = { shown = true }, modifier = Modifier.focusRequester(ok).focusRing()) { Text("OK") }
        },
        dismissButton = {
            TextButton(
                shape = Corner,
                onClick = {
                    shown = true
                    app.container.scope.launch { app.container.settings.update { it.copy(vpnNotice = false) } }
                },
                modifier = Modifier.focusRing(),
            ) { Text("Do not show again") }
        },
    )
    LaunchedEffect(Unit) { runCatching { ok.requestFocus() } }
}
