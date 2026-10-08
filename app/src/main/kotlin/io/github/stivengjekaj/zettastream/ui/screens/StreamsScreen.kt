package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
fun StreamsScreen(app: AppState, screen: Screen.Streams) {
    val c = app.container
    val tv = app.isTv
    val type = screen.meta.type
    val groups = remember(screen.videoId) { mutableStateListOf<StreamGroup>() }
    var done by remember(screen.videoId) { mutableStateOf(false) }
    val asked = remember(screen.videoId) { c.addons.addons.value.addons.count { it.supports("stream", type, screen.videoId) } }
    val subtitles by produceState(emptyList<Subtitle>(), screen.videoId) { value = c.addons.subtitles(type, screen.videoId) }
    val firstFocus = remember { FocusRequester() }
    var focused by remember(screen.videoId) { mutableStateOf(false) }

    LaunchedEffect(screen.videoId) {
        c.addons.streams(type, screen.videoId).collect { groups += it }
        done = true
    }
    val playable = groups.filter { it.streams.isNotEmpty() }
    LaunchedEffect(playable.size) {
        if (!focused && playable.isNotEmpty()) { focused = runCatching { firstFocus.requestFocus() }.isSuccess }
    }

    fun play(stream: Stream) {
        app.open(
            Screen.Player(
                VideoPlayback(
                    meta = screen.meta,
                    videoId = screen.videoId,
                    label = screen.label,
                    url = stream.url!!,
                    headers = stream.requestHeaders,
                    subtitles = (stream.subtitles + subtitles).distinctBy { it.url },
                    episodes = screen.episodes,
                ),
            ),
        )
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Sizes.gutter(tv))) {
        item {
            Text(screen.meta.name, color = TextPrimary, fontSize = if (tv) 30.sp else 22.sp, fontWeight = FontWeight.Bold)
            if (screen.label != screen.meta.name) Text(screen.label, color = TextSecondary, fontSize = 16.sp)
            Text(
                if (done) "${count(playable.sumOf { it.streams.size }, "stream")} from ${playable.size} of ${count(asked, "addon")}"
                else "Asking ${count(asked, "addon")} for streams",
                color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp),
            )
            if (!done) LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
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
                val hidden = groups.sumOf { it.hidden }
                Message(
                    "No playable stream",
                    if (hidden > 0) "${count(hidden, "stream")} had only a torrent and no HTTP link, so the app hides them. A debrid key in the addon settings changes torrents into HTTP links."
                    else "No addon found a stream for this video.",
                    tv, Modifier.padding(top = 16.dp),
                )
            }
        }
        var first = true
        groups.forEach { group ->
            item(key = "head-" + group.addon.manifestUrl) {
                Text(
                    group.addon.name + when {
                        group.error != null -> "  (no answer)"
                        group.hidden > 0 -> "  (${count(group.hidden, "torrent stream")} hidden)"
                        else -> ""
                    },
                    color = if (group.error != null) Danger else TextSecondary,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                )
            }
            items(group.streams, key = { group.addon.manifestUrl + "|" + it.url }) { stream ->
                val isFirst = first.also { first = false }
                val shape = Corner
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .then(if (isFirst) Modifier.focusRequester(firstFocus) else Modifier)
                        .focusRing(shape, scaleTo = 1.02f)
                        .clip(shape)
                        .background(SurfaceHigh)
                        .clickable { play(stream) }
                        .padding(14.dp),
                ) {
                    Text(stream.name?.replace('\n', ' ') ?: group.addon.name, color = TextPrimary, fontSize = if (tv) 18.sp else 15.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (stream.details.isNotBlank()) {
                        Text(stream.details, color = TextSecondary, fontSize = 13.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
