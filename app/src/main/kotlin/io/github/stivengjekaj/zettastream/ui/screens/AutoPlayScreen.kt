package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.addon.StreamGroup
import io.github.stivengjekaj.zettastream.addon.StreamOrder
import io.github.stivengjekaj.zettastream.addon.Subtitle
import io.github.stivengjekaj.zettastream.remote.RemoteAction
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.ui.videoPlayback
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** With an ideal stream, the app waits this long for a better answer from the other addons. */
private const val IDEAL_WAIT = 2_500L

/** The longest wait for slow addons when some stream is already there. */
private const val MAX_WAIT = 8_000L

/** The number of next best streams that the player keeps, in case a stream fails. */
private const val FALLBACKS = 5

/**
 * Asks each addon for streams and plays the best one at once. The player
 * takes the place of this screen, so Back goes to the episodes. Yellow shows
 * the list of streams instead.
 */
@Composable
fun AutoPlayScreen(app: AppState, screen: Screen.AutoPlay) {
    val c = app.container
    val tv = app.isTv
    val type = screen.meta.type
    val asked = remember(screen.videoId) { c.addons.addons.value.addons.count { it.supports("stream", type, screen.videoId) } }
    var answered by remember(screen.videoId) { mutableIntStateOf(0) }
    var failed by remember(screen.videoId) { mutableStateOf(false) }

    fun showList() = app.replace(Screen.Streams(screen.meta, screen.videoId, screen.label, screen.episodes))

    DisposableEffect(Unit) {
        app.screenActions = mapOf(RemoteAction.Sources to { showList() })
        onDispose { app.screenActions = emptyMap() }
    }

    LaunchedEffect(screen.videoId) {
        val settings = c.settings.settings.value
        val subtitles = async { runCatching { c.addons.subtitles(type, screen.videoId) }.getOrDefault(emptyList<Subtitle>()) }
        val groups = mutableListOf<StreamGroup>()
        var done = false
        val kept = c.addons.cachedStreams(type, screen.videoId, settings.showTorrents)
        if (kept != null) {
            groups += kept
            done = true
        } else {
            launch {
                c.addons.streams(type, screen.videoId, settings.showTorrents).collect { groups += it; answered++ }
                c.addons.keepStreams(type, screen.videoId, settings.showTorrents, groups.toList())
                done = true
            }
        }
        // Start with an ideal stream soon, with any stream after MAX_WAIT, or when all addons answered.
        val start = System.currentTimeMillis()
        while (!done) {
            val waited = System.currentTimeMillis() - start
            val best = StreamOrder.best(groups.toList(), settings.directFirst)
            if (best != null && StreamOrder.isIdeal(best, settings.directFirst) && waited >= IDEAL_WAIT) break
            if (best != null && waited >= MAX_WAIT) break
            delay(250)
        }
        val ranked = StreamOrder.ranked(groups.toList(), settings.directFirst)
        if (ranked.isEmpty()) { failed = true; return@LaunchedEffect }
        val subs = withTimeoutOrNull(2_000) { subtitles.await() }.orEmpty()
        app.replace(
            Screen.Player(
                videoPlayback(
                    screen.meta, screen.videoId, screen.label, screen.episodes, ranked.first(), subs,
                    fallbacks = ranked.drop(1).take(FALLBACKS),
                ),
            ),
        )
    }

    Column(
        Modifier.fillMaxSize().padding(Sizes.gutter(tv)),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(screen.meta.name, color = TextPrimary, fontSize = if (tv) 30.sp else 22.sp, fontWeight = FontWeight.Bold)
        if (screen.label != screen.meta.name) Text(screen.label, color = TextSecondary, fontSize = 16.sp)
        if (failed) {
            Text(
                if (asked == 0) "No addon gives streams for this title. Add one in Settings, Sources."
                else "No addon found a playable stream. Press Yellow to see the list, or Back.",
                color = TextPrimary, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            Text(
                "Finding the best stream: ${count(answered, "addon")} of $asked answered",
                color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp),
            )
            LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            ZButton(onClick = ::showList, tv = tv, modifier = Modifier.padding(top = 8.dp)) { Text(if (tv) "Choose from the list (Yellow)" else "Choose from the list") }
        }
    }
}
