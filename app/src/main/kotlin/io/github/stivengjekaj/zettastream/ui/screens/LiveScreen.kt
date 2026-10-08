package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.stivengjekaj.zettastream.iptv.Channel
import io.github.stivengjekaj.zettastream.iptv.Guide
import io.github.stivengjekaj.zettastream.source.SourceKind
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.LivePlayback
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Message
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Surface
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Background
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import java.text.DateFormat
import java.util.Date

/** The groups to show first: at most 40, with the largest first. */
private fun groupsOf(channels: List<Channel>): List<String> =
    channels.mapNotNull { it.group }.groupingBy { it }.eachCount().entries
        .sortedByDescending { it.value }.take(40).map { it.key }

@Composable
fun LiveScreen(app: AppState) {
    val c = app.container
    val tv = app.isTv
    val state by c.live.live.collectAsState()
    val sources by c.sources.sources.collectAsState()
    var group by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.live.ensureLoaded() }

    val groups = remember(state.channels) { groupsOf(state.channels) }
    val shown = remember(state.channels, group) { state.channels.filter { group == null || it.group == group } }
    val now = System.currentTimeMillis()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = if (tv) 32.dp else 16.dp)) {
        item {
            Text("Live TV", color = TextPrimary, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv)))
            Text(
                when {
                    state.loading -> "Loading ${state.channels.size} channels and the guide"
                    else -> "${state.channels.size} channels"
                },
                color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = Sizes.gutter(tv)),
            )
            if (state.loading) LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth().padding(horizontal = Sizes.gutter(tv), vertical = 8.dp))
        }
        if (sources.none { it.kind == SourceKind.Playlist }) {
            item {
                Message(
                    "No playlists",
                    "Add an M3U playlist in Settings, Sources to watch live TV. Add an XMLTV guide to see what is on now.",
                    tv,
                )
            }
        }
        if (groups.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(listOf<String?>(null) + groups) { g ->
                        FilterChip(
                            shape = Corner,
                            selected = g == group,
                            onClick = { group = g },
                            label = { Text(g ?: "All") },
                            modifier = Modifier.focusRing(Corner, scaleTo = 1.04f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Outline, selectedLabelColor = TextPrimary,
                                containerColor = Surface, labelColor = TextSecondary,
                            ),
                        )
                    }
                }
            }
        }
        items(shown.size, key = { shown[it].url }) { index ->
            val channel = shown[index]
            ChannelRow(channel, state.guide, now, tv) { app.open(Screen.Player(LivePlayback(shown, index))) }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, guide: Guide, now: Long, tv: Boolean, onClick: () -> Unit) {
    val shape = Corner
    val (current, next) = guide.nowAndNext(channel.tvgId, now)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter(tv), vertical = 4.dp)
            .focusRing(shape, scaleTo = 1.02f)
            .clip(shape)
            .background(SurfaceHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 96.dp, height = 54.dp).clip(RoundedCornerShape(8.dp)).background(Background), contentAlignment = Alignment.Center) {
            Text(channel.name.take(3), color = TextSecondary, fontSize = 14.sp)
            if (channel.logo != null) AsyncImage(channel.logo, null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(4.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = TextPrimary, fontSize = if (tv) 18.sp else 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (current != null) {
                Text("${time.format(Date(current.start))}  ${current.title}", color = TextPrimary.copy(alpha = 0.85f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                LinearProgressIndicator(
                    progress = { ((now - current.start).toFloat() / (current.stop - current.start)).coerceIn(0f, 1f) },
                    color = Accent, trackColor = Background,
                    modifier = Modifier.fillMaxWidth(0.6f).height(3.dp).padding(top = 2.dp),
                )
            }
            if (next != null) {
                Text("${time.format(Date(next.start))}  ${next.title}", color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else if (current == null && channel.group != null) {
                Text(channel.group, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}
