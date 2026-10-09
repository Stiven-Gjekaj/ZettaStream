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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import io.github.stivengjekaj.zettastream.ui.components.ScrollingText
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.stivengjekaj.zettastream.addon.Meta
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.addon.Video
import io.github.stivengjekaj.zettastream.library.LibraryData
import io.github.stivengjekaj.zettastream.remote.RemoteAction
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.isAnime
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.TvRow
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Surface
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Background
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** The season number for the list. Specials (season 0) go last. */
private fun seasonKey(season: Int?) = when (season) { null -> Int.MAX_VALUE - 1; 0 -> Int.MAX_VALUE; else -> season }

/** Removes "Season 2", "2nd Season", or "Part 2" from the end of a name. */
val seasonSuffix = Regex("""\s*(:\s*)?(Season\s*\d+|\d+(st|nd|rd|th)\s+Season|Part\s*\d+)$""", RegexOption.IGNORE_CASE)

fun episodeLabel(v: Video): String = buildString {
    if (v.season != null && v.episode != null) append("S${v.season} E${v.episode}  ")
    append(v.label)
}

/** Selects the episode that Play starts: the one to resume, or the first one not watched. */
fun nextToPlay(meta: Meta, data: LibraryData): Video? {
    val videos = meta.videos.sortedWith(compareBy({ seasonKey(it.season) }, { it.episode ?: 0 }))
    val resume = videos.mapNotNull { v -> data.progress[v.id]?.takeIf { !it.isFinished && it.position > 0 }?.let { v to it } }
        .maxByOrNull { it.second.updated }?.first
    return resume ?: videos.firstOrNull { it.id !in data.watched && seasonKey(it.season) != Int.MAX_VALUE } ?: videos.firstOrNull()
}

@Composable
fun DetailScreen(app: AppState, preview: MetaPreview) {
    val c = app.container
    val tv = app.isTv
    val scope = rememberCoroutineScope()
    val library by c.library.data.collectAsState()
    val addonState by c.addons.addons.collectAsState()
    val meta by produceState<Meta?>(null, preview.id) { value = c.addons.meta(preview) }
    val m = meta
    var season by remember { mutableStateOf<Int?>(null) }
    var focusedVideo by remember { mutableStateOf<Video?>(null) }
    val playFocus = remember { FocusRequester() }

    val isSeries = m != null && m.videos.isNotEmpty() && m.type != "movie"
    val seasons = m?.videos?.map { it.season }?.distinct()?.sortedBy { seasonKey(it) }.orEmpty()
    val target = if (m != null && isSeries) nextToPlay(m, library) else null
    val selectedSeason = season ?: target?.season ?: seasons.firstOrNull()
    val episodes = m?.videos?.filter { it.season == selectedSeason }?.sortedBy { it.episode ?: 0 }.orEmpty()
    val allEpisodes = m?.videos?.sortedWith(compareBy({ seasonKey(it.season) }, { it.episode ?: 0 })).orEmpty()

    fun play(video: Video?) {
        val base = m?.toPreview() ?: preview
        // An anime plays the best stream at once. The list of streams stays one key away.
        val auto = isAnime(base)
        if (video == null) app.open(Screen.Streams(base, base.id, base.name, emptyList(), autoPlay = auto))
        else app.open(Screen.Streams(base, video.id, episodeLabel(video), allEpisodes, autoPlay = auto))
    }

    DisposableEffect(m, focusedVideo, target) {
        app.screenActions = mapOf(
            RemoteAction.Watchlist to { app.toggleWatchlist(m?.toPreview() ?: preview) },
            RemoteAction.Watched to {
                val id = focusedVideo?.id ?: if (isSeries) null else preview.id
                if (id != null) scope.launch { c.library.toggleWatched(id) }
            },
            RemoteAction.Sources to { play(focusedVideo ?: target) },
            RemoteAction.Info to {},
        )
        onDispose { app.screenActions = emptyMap() }
    }
    LaunchedEffect(m != null) { if (m != null) runCatching { playFocus.requestFocus() } }

    Box(Modifier.fillMaxSize()) {
        val background = m?.background ?: preview.background ?: m?.poster ?: preview.poster
        if (background != null) {
            AsyncImage(background, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(), alpha = 0.35f)
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Background.copy(alpha = 0.3f), Background))))

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Sizes.gutter(tv))) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    val poster = m?.poster ?: preview.poster
                    if (poster != null) {
                        AsyncImage(
                            poster, null, contentScale = ContentScale.Crop,
                            modifier = Modifier.width(if (tv) 200.dp else 110.dp).height(if (tv) 300.dp else 165.dp)
                                .clip(Corner),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(m?.name ?: preview.name, color = TextPrimary, fontSize = if (tv) 36.sp else 24.sp, fontWeight = FontWeight.Bold)
                        val info = listOfNotNull(m?.releaseInfo ?: preview.releaseInfo, m?.runtime, (m?.imdbRating ?: preview.imdbRating)?.let { "IMDb $it" })
                        if (info.isNotEmpty()) Text(info.joinToString("  ·  "), color = TextSecondary, fontSize = 15.sp)
                        val genres = m?.genres ?: preview.genres
                        if (genres.isNotEmpty()) Text(genres.take(4).joinToString(", "), color = TextSecondary, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            m?.description ?: preview.description.orEmpty(), color = TextPrimary, fontSize = if (tv) 17.sp else 14.sp,
                            maxLines = if (tv) 5 else 8, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(18.dp))
                        if (m == null) CircularProgressIndicator(color = Accent)
                        else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            val resume = target?.let { library.progress[it.id] }?.takeIf { !it.isFinished && it.position > 0 }
                            ZButton(
                                onClick = { play(if (isSeries) target else null) },
                                tv = tv,
                                primary = true,
                                modifier = Modifier.focusRequester(playFocus),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    when {
                                        !isSeries -> if (library.progress[preview.id]?.let { !it.isFinished && it.position > 0 } == true) "Resume" else "Play"
                                        target != null -> (if (resume != null) "Resume " else "Play ") +
                                            (if (target.season != null && target.episode != null) "S${target.season} E${target.episode}" else target.label)
                                        else -> "Play"
                                    },
                                )
                            }
                            // Anime Kitsu gives each season as its own title. The IMDb ID opens the
                            // whole show under one name, with every season.
                            val whole = m.imdbId?.takeIf { m.id.startsWith("kitsu:") && m.type != "movie" }
                            if (whole != null && addonState.addons.any { it.supports("meta", "series", whole) }) {
                                ZButton(
                                    onClick = { app.open(Screen.Detail(MetaPreview(whole, "series", m.name.replace(seasonSuffix, ""), m.poster))) },
                                    tv = tv,
                                ) { Text("All seasons") }
                            }
                            val inList = library.watchlist.any { it.id == preview.id }
                            ZButton(onClick = { app.toggleWatchlist(m.toPreview()) }, tv = tv) {
                                Icon(if (inList) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder, null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (inList) "In watchlist" else "Watchlist")
                            }
                            if (!isSeries) {
                                val watched = preview.id in library.watched
                                ZButton(onClick = { scope.launch { c.library.toggleWatched(preview.id) } }, tv = tv) {
                                    Icon(if (watched) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (watched) "Watched" else "Mark watched")
                                }
                            }
                        }
                        if (tv) {
                            Spacer(Modifier.height(10.dp))
                            Text("Red: watchlist   Green: watched   Yellow: sources", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
            if (isSeries && seasons.size > 1) {
                item {
                    TvRow(tv) {
                        LazyRow(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(seasons) { s ->
                                FilterChip(
                                    shape = Corner,
                                    selected = s == selectedSeason,
                                    onClick = { season = s },
                                    label = { Text(when (s) { null -> "Other"; 0 -> "Specials"; else -> "Season $s" }) },
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
            }
            if (isSeries) {
                item { Spacer(Modifier.height(12.dp)) }
                items(episodes, key = { it.id }) { video ->
                    EpisodeRow(video, tv, library, onFocus = { focusedVideo = video }, onClick = { play(video) })
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(video: Video, tv: Boolean, library: LibraryData, onFocus: () -> Unit, onClick: () -> Unit) {
    val shape = Corner
    val progress = library.progress[video.id]
    val watched = video.id in library.watched
    var rowFocused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .onFocusChanged { rowFocused = it.isFocused }
            .focusRing(shape, scaleTo = 1.02f, onFocus = onFocus)
            .clip(shape)
            .background(SurfaceHigh.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(if (tv) 200.dp else 120.dp).height(if (tv) 112.dp else 68.dp).clip(RoundedCornerShape(8.dp)).background(Background)) {
            if (video.thumbnail != null) AsyncImage(video.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (progress != null && progress.duration > 0 && !progress.isFinished) {
                Box(
                    Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.position.toFloat() / progress.duration)
                        .height(4.dp).background(Accent),
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            ScrollingText(episodeLabel(video), active = rowFocused, color = TextPrimary, fontSize = if (tv) 18.sp else 15.sp,
                lineHeight = if (tv) 24.sp else 20.sp, maxLines = 1)
            video.released?.take(10)?.let { Text(it, color = TextSecondary, fontSize = 12.sp) }
            video.summary?.let { Text(it, color = TextSecondary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        if (watched) Icon(Icons.Rounded.CheckCircle, "Watched", tint = Accent, modifier = Modifier.size(24.dp))
    }
}
