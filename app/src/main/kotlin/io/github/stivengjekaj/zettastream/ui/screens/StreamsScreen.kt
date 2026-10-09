package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import io.github.stivengjekaj.zettastream.addon.ListedStream
import io.github.stivengjekaj.zettastream.addon.StreamInfo
import io.github.stivengjekaj.zettastream.addon.StreamOrder
import io.github.stivengjekaj.zettastream.remote.RemoteAction
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.stivengjekaj.zettastream.ui.components.ScrollingText
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.addon.Stream
import io.github.stivengjekaj.zettastream.addon.StreamGroup
import io.github.stivengjekaj.zettastream.addon.Subtitle
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.VideoPlayback
import io.github.stivengjekaj.zettastream.ui.videoPlayback
import io.github.stivengjekaj.zettastream.ui.theme.OnAccent
import io.github.stivengjekaj.zettastream.ui.components.Message
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Danger
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary

/** Writes a count with the correct form of the word: 1 stream, 2 streams. */
fun count(n: Int, word: String): String = "$n $word" + if (n == 1) "" else "s"

@Composable
private fun Badge(text: String, strong: Boolean = false) {
    Text(
        text, color = if (strong) OnAccent else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(end = if (strong) 10.dp else 0.dp).clip(Corner)
            .background(if (strong) TextSecondary else Outline).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun StreamsScreen(app: AppState, screen: Screen.Streams) {
    val c = app.container
    val tv = app.isTv
    val type = screen.meta.type
    val viewer by c.settings.settings.collectAsState()
    var reloads by remember(screen.videoId) { mutableIntStateOf(0) }
    val groups = remember(screen.videoId, reloads) { mutableStateListOf<StreamGroup>() }
    var done by remember(screen.videoId, reloads) { mutableStateOf(false) }
    val asked = remember(screen.videoId) { c.addons.addons.value.addons.count { it.supports("stream", type, screen.videoId) } }
    val subtitles by produceState(emptyList<Subtitle>(), screen.videoId) { value = c.addons.subtitles(type, screen.videoId) }
    val firstFocus = remember { FocusRequester() }
    var focused by remember(screen.videoId) { mutableStateOf(false) }
    // The row that had the focus. Back from the player gives it the focus again.
    var lastRow by rememberSaveable(screen.videoId) { mutableStateOf<String?>(null) }

    LaunchedEffect(screen.videoId, reloads) {
        val withTorrents = c.settings.settings.value.showTorrents
        // Back from the player shows the same list at once. Reload asks the addons again.
        val kept = if (reloads == 0) c.addons.cachedStreams(type, screen.videoId, withTorrents) else null
        if (kept != null) groups += kept
        else {
            c.addons.streams(type, screen.videoId, withTorrents).collect { groups += it }
            c.addons.keepStreams(type, screen.videoId, withTorrents, groups.toList())
        }
        done = true
    }
    // Blue asks every addon again. A slow addon sometimes answers with fewer streams.
    DisposableEffect(Unit) {
        app.screenActions = mapOf(RemoteAction.Filter to { reloads++ })
        onDispose { app.screenActions = emptyMap() }
    }
    val playable = groups.filter { it.streams.isNotEmpty() }
    // An addon can give more than 1000 torrents. The list is made one time for each answer, not for each key press.
    val sections = remember(groups.size, viewer.directFirst) { StreamOrder.sections(groups.toList(), viewer.directFirst) }
    fun rowKey(section: String, listed: ListedStream) =
        section + "|" + listed.addon.manifestUrl + "|" + (listed.stream.url ?: listed.stream.infoHash + ":" + listed.stream.fileIdx)
    val keys = remember(sections) { sections.flatMap { sec -> sec.streams.map { rowKey(sec.title, it) } }.toSet() }
    // The row of the last visit, read without a subscription, so that a focus move does not make the list again.
    val returnRow = remember { Snapshot.withoutReadObservation { lastRow } }
    // The focus goes to the row of the last visit, or else to the first stream.
    val focusKey = returnRow?.takeIf { it in keys } ?: sections.firstOrNull()?.let { sec -> sec.streams.firstOrNull()?.let { rowKey(sec.title, it) } }
    // The list adds a row one frame after it shows, so wait for one frame.
    val listState = rememberLazyListState()
    // The place of a row in the list: the header item, then a heading and the rows of each section.
    fun rowIndex(key: String): Int {
        var index = 1
        sections.forEach { sec ->
            index++
            sec.streams.forEach { if (rowKey(sec.title, it) == key) return index; index++ }
        }
        return 0
    }
    LaunchedEffect(focusKey) {
        if (!focused && focusKey != null) {
            withFrameNanos { }
            // A row out of view has no node to focus. Scroll to it first.
            if (listState.layoutInfo.visibleItemsInfo.none { it.key == focusKey }) {
                listState.scrollToItem(rowIndex(focusKey))
                withFrameNanos { }
            }
            focused = runCatching { firstFocus.requestFocus() }.getOrDefault(false)
        }
    }

    fun play(listed: ListedStream) {
        app.open(Screen.Player(videoPlayback(screen.meta, screen.videoId, screen.label, screen.episodes, listed, subtitles)))
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Sizes.gutter(tv)),
    ) {
        item {
            Text(screen.meta.name, color = TextPrimary, fontSize = if (tv) 30.sp else 22.sp, fontWeight = FontWeight.Bold)
            if (screen.label != screen.meta.name) Text(screen.label, color = TextSecondary, fontSize = 16.sp)
            Text(
                if (done) "${count(playable.sumOf { it.streams.size }, "stream")} from ${playable.size} of ${count(asked, "addon")}"
                else "Asking ${count(asked, "addon")} for streams",
                color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp),
            )
            if (!done) LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ZButton(onClick = { reloads++ }, tv = tv) { Text("Reload streams") }
                ZButton(onClick = { c.scope.launch { c.settings.update { it.copy(directFirst = !it.directFirst) } } }, tv = tv) {
                    Text(if (viewer.directFirst) "Direct links first: on" else "Direct links first: off")
                }
            }
            if (tv) Text("Blue: reload", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        if (asked == 0) {
            item {
                Message(
                    "No addon gives streams for this title",
                    "Add an addon that gives streams in Settings, Sources. The addon must accept IDs like ${screen.videoId.substringBefore(':')}.",
                    tv, Modifier.padding(top = 16.dp),
                )
            }
        } else if (done && playable.isEmpty()) {
            item {
                val hiddenTorrents = groups.sumOf { it.hiddenTorrents }
                Message(
                    "No playable stream",
                    if (hiddenTorrents > 0) "${count(hiddenTorrents, "torrent stream")} are hidden. They show when Settings, Show torrent streams is on."
                    else "No addon found a stream for this video.",
                    tv, Modifier.padding(top = 16.dp),
                )
            }
        }
        // Addons that did not answer, or that hide torrents, show under the list.
        val notes = groups.filter { it.error != null || it.hiddenTorrents > 0 }
        sections.forEach { section ->
            item(key = "head-" + section.title) {
                Text(
                    section.title, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                )
            }
            items(section.streams, key = { rowKey(section.title, it) }) { listed ->
                val stream = listed.stream
                var rowFocused by remember { mutableStateOf(false) }
                val key = rowKey(section.title, listed)
                val shape = Corner
                val rowModifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .then(if (key == focusKey) Modifier.focusRequester(firstFocus) else Modifier)
                    .onFocusChanged { rowFocused = it.isFocused; if (it.isFocused) lastRow = key }
                    .focusRing(shape, scaleTo = 1.02f)
                    .clip(shape)
                    .background(SurfaceHigh)
                    .clickable { play(listed) }
                    .padding(14.dp)
                val summary: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (stream.isTorrent) Badge("TORRENT", strong = true)
                        ScrollingText(
                            stream.name?.replace('\n', ' ') ?: listed.addon.name, active = rowFocused, color = TextPrimary,
                            fontSize = if (tv) 18.sp else 15.sp, lineHeight = if (tv) 24.sp else 20.sp, maxLines = 2, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    val badges = listed.info.badges
                    if (badges.isNotEmpty()) {
                        FlowRow(
                            Modifier.padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) { badges.forEach { Badge(it) } }
                    }
                    Text(listed.addon.name, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
                // All the text of the addon shows. The focused row shows it in full.
                val details: @Composable (Modifier) -> Unit = { modifier ->
                    if (stream.details.isNotEmpty()) {
                        Text(
                            stream.details, color = TextSecondary, fontSize = if (tv) 14.sp else 13.sp,
                            maxLines = if (rowFocused) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis, modifier = modifier,
                        )
                    }
                }
                if (tv) {
                    // On a TV, the details fill the space on the right of the row.
                    Row(rowModifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(Modifier.weight(0.42f)) { summary() }
                        details(Modifier.weight(0.58f))
                    }
                } else {
                    Column(rowModifier) {
                        summary()
                        details(Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        items(notes, key = { "note-" + it.addon.manifestUrl }) { group ->
            Text(
                group.addon.name + if (group.error != null) ": no answer" else ": ${count(group.hiddenTorrents, "torrent stream")} hidden",
                color = if (group.error != null) Danger else TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
