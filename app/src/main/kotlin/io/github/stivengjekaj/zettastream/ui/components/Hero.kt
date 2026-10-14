package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.ui.FocusMemory
import io.github.stivengjekaj.zettastream.ui.typeLabel
import io.github.stivengjekaj.zettastream.ui.theme.Background
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import kotlinx.coroutines.delay

/** The time that each title of the banner shows, while no button of the banner has the focus. */
private const val SLIDE_TIME = 8_000L

/** The start of the FocusMemory key of a banner button. The number of the button follows. */
private const val BANNER_KEY = "banner|"

/**
 * The large banner at the top of Home, after Miruro 2.0: the art of a title,
 * its logo or name, its facts, a short text, and three buttons.
 */
@Composable
fun HeroBanner(
    items: List<MetaPreview>,
    tv: Boolean,
    inWatchlist: (MetaPreview) -> Boolean,
    onPlay: (MetaPreview) -> Unit,
    onDetails: (MetaPreview) -> Unit,
    onWatchlist: (MetaPreview) -> Unit,
    takeFocus: Boolean = false,
    onFocusTaken: () -> Unit = {},
) {
    if (items.isEmpty()) return
    val buttons = remember { List(3) { FocusRequester() } }
    // After Back, the button that had the focus takes it again. On a TV, the first button
    // takes the focus when the screen opens, so the first key press has a place to start.
    LaunchedEffect(Unit) {
        val back = FocusMemory.pending?.removePrefix(BANNER_KEY)?.toIntOrNull()
        if (back != null) FocusMemory.pending = null
        val target = back ?: if (tv && takeFocus) 0 else return@LaunchedEffect
        withFrameNanos { }
        if (runCatching { buttons[target].requestFocus() }.isSuccess) onFocusTaken()
    }
    fun remembered(i: Int) = Modifier.focusRequester(buttons[i]).onFocusChanged { if (it.hasFocus) FocusMemory.last = "$BANNER_KEY$i" }
    var index by rememberSaveable { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(false) }
    // The banner moves on by itself, but not while the viewer is on one of its buttons.
    LaunchedEffect(items.size, focused) {
        while (!focused && items.size > 1) {
            delay(SLIDE_TIME)
            index = (index + 1) % items.size
        }
    }
    val meta = items[index % items.size]
    Box(Modifier.fillMaxWidth().height(if (tv) 470.dp else 320.dp)) {
        Crossfade(meta, label = "banner") { m ->
            AsyncImage(m.background ?: m.poster, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
        }
        // The text stays easy to read on any art.
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Background.copy(alpha = 0.92f), 0.55f to Background.copy(alpha = 0.35f), 1f to Background.copy(alpha = 0f))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Background.copy(alpha = 0f), 1f to Background)))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(if (tv) 0.5f else 0.9f).padding(start = Sizes.gutter(tv), bottom = if (tv) 36.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (meta.logo != null) {
                AsyncImage(meta.logo, meta.name, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart, modifier = Modifier.height(if (tv) 110.dp else 70.dp).fillMaxWidth(0.8f))
            } else {
                Text(meta.name, color = TextPrimary, fontSize = if (tv) 44.sp else 28.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOfNotNull(meta.releaseInfo, typeLabel(meta.type)) + meta.genres.take(3)).forEach { FactChip(it) }
            }
            meta.description?.let {
                Text(it, color = TextPrimary.copy(alpha = 0.85f), fontSize = if (tv) 16.sp else 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(
                Modifier.onFocusChanged { focused = it.hasFocus },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ZButton(onClick = { onPlay(meta) }, tv = tv, primary = true, modifier = remembered(0)) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Text("Watch now", modifier = Modifier.padding(start = 6.dp))
                }
                ZButton(onClick = { onDetails(meta) }, tv = tv, modifier = remembered(1)) {
                    Icon(Icons.Rounded.Info, null, modifier = Modifier.size(20.dp))
                    Text("Details", modifier = Modifier.padding(start = 6.dp))
                }
                ZButton(onClick = { onWatchlist(meta) }, tv = tv, modifier = remembered(2)) {
                    Icon(if (inWatchlist(meta)) Icons.Rounded.Check else Icons.Rounded.Add, "Watchlist", modifier = Modifier.size(20.dp))
                }
            }
        }
        // One mark for each title. The mark of the title on screen is long and white.
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = if (tv) 18.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items.indices.forEach { i ->
                val width by animateDpAsState(if (i == index % items.size) 26.dp else 10.dp, label = "dot")
                Box(Modifier.width(width).height(4.dp).clip(Corner).background(if (i == index % items.size) TextPrimary else Outline))
            }
        }
    }
}

/** A small fact, such as the year or a genre, in a thin frame. */
@Composable
fun FactChip(text: String) {
    Text(
        text, color = TextSecondary, fontSize = 13.sp, maxLines = 1,
        modifier = Modifier.clip(Corner).border(1.dp, Outline, Corner).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
