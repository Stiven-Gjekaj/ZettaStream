package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import io.github.stivengjekaj.zettastream.addon.Display
import io.github.stivengjekaj.zettastream.ui.components.FactChip
import io.github.stivengjekaj.zettastream.ui.theme.OnAccent
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.withFrameNanos
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
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.ZChip
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

    // Play starts the best stream at once. Yellow shows the list of streams.
    fun play(video: Video?, choose: Boolean = false) {
        val base = m?.toPreview() ?: preview
        val id = video?.id ?: base.id
        val label = video?.let(::episodeLabel) ?: base.name
        val episodes = if (video == null) emptyList() else allEpisodes
        app.open(if (choose) Screen.Streams(base, id, label, episodes) else Screen.AutoPlay(base, id, label, episodes))
    }

    DisposableEffect(m, focusedVideo, target) {
        app.screenActions = mapOf(
            RemoteAction.Watchlist to { app.toggleWatchlist(m?.toPreview() ?: preview) },
            RemoteAction.Watched to {
                val id = focusedVideo?.id ?: if (isSeries) null else preview.id
                if (id != null) scope.launch { c.library.toggleWatched(id) }
            },
            RemoteAction.Sources to { play(focusedVideo ?: target, choose = true) },
            RemoteAction.Info to {},
        )
        onDispose { app.screenActions = emptyMap() }
    }
    val list = rememberLazyListState()
    // Down from the buttons goes to the episode that Watch starts, not to the card below the button.
    val episodeFocus = remember { FocusRequester() }
    val downTarget = episodes.firstOrNull { it.id == target?.id } ?: episodes.firstOrNull()
    // The card of a far episode is not built yet. Then Down uses the normal search.
    var downTargetShown by remember { mutableStateOf(false) }
    LaunchedEffect(m != null) {
        if (m != null) {
            runCatching { playFocus.requestFocus() }
            // The page opens at the top, so that the title shows. The focus scrolls the page
            // in the next frames, so this waits for that scroll first.
            repeat(3) { withFrameNanos { } }
            list.scrollToItem(0)
        }
    }

    Box(Modifier.fillMaxSize()) {
        // The art fills the top of the page, as on Miruro 2.0. It fades into the black at the left and below.
        val background = m?.background ?: preview.background ?: m?.poster ?: preview.poster
        Box(Modifier.fillMaxWidth().height(if (tv) 520.dp else 300.dp)) {
            if (background != null) {
                AsyncImage(background, null, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize(), alpha = 0.7f)
            }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Background.copy(alpha = 0.95f), 0.6f to Background.copy(alpha = 0.4f), 1f to Background.copy(alpha = 0.1f))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Background.copy(alpha = 0f), 1f to Background)))
        }

        LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = PaddingValues(Sizes.gutter(tv))) {
            item {
                Row(Modifier.padding(top = if (tv) 40.dp else 8.dp), horizontalArrangement = Arrangement.spacedBy(if (tv) 32.dp else 16.dp)) {
                    val poster = m?.poster ?: preview.poster
                    if (poster != null) {
                        AsyncImage(
                            poster, null, contentScale = ContentScale.Crop,
                            modifier = Modifier.width(if (tv) 220.dp else 110.dp).height(if (tv) 330.dp else 165.dp)
                                .clip(Corner),
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val logo = m?.logo ?: preview.logo
                        val name = m?.name ?: preview.name
                        if (logo != null && tv) {
                            AsyncImage(logo, name, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart, modifier = Modifier.height(100.dp).fillMaxWidth(0.6f))
                        } else {
                            Text(name, color = TextPrimary, fontSize = if (tv) 40.sp else 24.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        val facts = listOfNotNull(
                            Display.year(m?.releaseInfo ?: preview.releaseInfo),
                            Display.kind(m?.type ?: preview.type),
                            m?.runtime,
                            Display.rating(m?.imdbRating ?: preview.imdbRating)?.let { "IMDb $it" },
                        ) + (m?.genres ?: preview.genres).take(3)
                        if (facts.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { facts.forEach { FactChip(it) } }
                        }
                        Text(
                            m?.description ?: preview.description.orEmpty(), color = TextPrimary.copy(alpha = 0.85f), fontSize = if (tv) 17.sp else 14.sp,
                            maxLines = if (tv) 4 else 8, overflow = TextOverflow.Ellipsis,
                        )
                        if (m == null) CircularProgressIndicator(color = Accent)
                        else Row(
                            Modifier.focusProperties { if (isSeries && seasons.size <= 1 && downTargetShown) down = episodeFocus },
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
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
                                        !isSeries -> if (library.progress[preview.id]?.let { !it.isFinished && it.position > 0 } == true) "Resume" else "Watch now"
                                        target != null -> (if (resume != null) "Resume " else "Watch ") +
                                            (if (target.season != null && target.episode != null) "S${target.season} E${target.episode}" else target.label)
                                        else -> "Watch now"
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
                                Icon(if (inList) Icons.Rounded.Check else Icons.Rounded.Add, null)
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
                        if (tv) Text("Red: watchlist   Green: watched   Yellow: sources", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            if (isSeries) {
                item { SectionHeading("Episodes", detail = "${count(episodes.size, "episode")}${selectedSeason?.let { if (it == 0) " · Specials" else " · Season $it" }.orEmpty()}", tv = tv) }
            }
            if (isSeries && seasons.size > 1) {
                item {
                    TvRow(tv) {
                        LazyRow(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(seasons) { s ->
                                ZChip(when (s) { null -> "Other"; 0 -> "Specials"; else -> "Season $s" }, selected = s == selectedSeason, tv = tv, onClick = { season = s })
                            }
                        }
                    }
                }
            }
            if (isSeries) {
                // A grid of episode cards: four on each line on a TV, two on a phone.
                val perLine = if (tv) 4 else 2
                items(episodes.chunked(perLine), key = { line -> line.first().id }) { line ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(if (tv) 20.dp else 12.dp)) {
                        line.forEach { video ->
                            EpisodeCard(video, tv, library, Modifier.weight(1f), if (video == downTarget) episodeFocus else null, { downTargetShown = it }, onFocus = { focusedVideo = video }, onClick = { play(video) })
                        }
                        repeat(perLine - line.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            if (m != null) {
                val about = aboutFacts(m)
                if (about.isNotEmpty()) {
                    item { SectionHeading("About", detail = null, tv = tv) }
                    item { AboutTable(about, tv) }
                }
            }
        }
    }
}

/** The facts for the About table, as label and value. A fact that the addon does not give does not show. */
fun aboutFacts(m: Meta): List<Pair<String, String>> {
    val seasons = m.videos.mapNotNull { it.season }.filter { it > 0 }.distinct().size
    return listOfNotNull(
        Display.kind(m.type)?.let { "Type" to it },
        m.releaseInfo?.let { "Released" to it },
        m.runtime?.let { "Runtime" to it },
        Display.rating(m.imdbRating)?.let { "IMDb" to "$it / 10" },
        m.genres.takeIf { it.isNotEmpty() }?.let { "Genres" to it.joinToString(", ") },
        seasons.takeIf { it > 0 && m.type != "movie" }?.let { "Seasons" to it.toString() },
        m.videos.size.takeIf { it > 0 && m.type != "movie" }?.let { "Episodes" to it.toString() },
        m.aliases.filter { it != m.name }.take(2).takeIf { it.isNotEmpty() }?.let { "Other names" to it.joinToString(", ") },
    )
}

@Composable
private fun SectionHeading(text: String, detail: String?, tv: Boolean) {
    Column(Modifier.padding(top = 32.dp, bottom = 12.dp)) {
        Text(text, color = TextPrimary, fontSize = if (tv) 24.sp else 19.sp, fontWeight = FontWeight.Bold)
        if (detail != null) Text(detail, color = TextSecondary, fontSize = 13.sp)
    }
}

@Composable
private fun AboutTable(facts: List<Pair<String, String>>, tv: Boolean) {
    // The table takes the focus, so that the remote can scroll down to it.
    Column(Modifier.fillMaxWidth(if (tv) 0.6f else 1f).focusRing(Corner, scaleTo = 1f).focusable().clip(Corner).border(1.dp, Outline, Corner)) {
        facts.forEachIndexed { i, (label, value) ->
            Row(Modifier.fillMaxWidth().background(if (i % 2 == 0) Surface else Background).padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(label, color = TextSecondary, fontSize = 14.sp, modifier = Modifier.width(if (tv) 160.dp else 110.dp))
                Text(value, color = TextPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    video: Video, tv: Boolean, library: LibraryData, modifier: Modifier, requester: FocusRequester?,
    onShown: (Boolean) -> Unit, onFocus: () -> Unit, onClick: () -> Unit,
) {
    if (requester != null) {
        DisposableEffect(requester) {
            onShown(true)
            onDispose { onShown(false) }
        }
    }
    val progress = library.progress[video.id]
    val watched = video.id in library.watched
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .focusRing(Corner, scaleTo = 1.04f, onFocus = onFocus)
                .clip(Corner)
                .background(SurfaceHigh)
                .clickable(onClick = onClick),
        ) {
            if (video.thumbnail != null) AsyncImage(video.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            // The episode number sits on the picture, as on Miruro 2.0.
            video.episode?.let {
                Text(
                    "EP $it", color = OnAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(Corner).background(Accent).padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            if (watched) {
                Icon(
                    Icons.Rounded.CheckCircle, "Watched", tint = Accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(22.dp).clip(CircleShape).background(Background),
                )
            }
            if (progress != null && progress.duration > 0 && !progress.isFinished) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Background.copy(alpha = 0.6f)))
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.position.toFloat() / progress.duration).height(4.dp).background(Accent))
            }
        }
        Spacer(Modifier.height(10.dp))
        ScrollingText(video.label, active = focused, color = TextPrimary, fontSize = if (tv) 15.sp else 14.sp, lineHeight = 20.sp, maxLines = 1, fontWeight = FontWeight.Medium)
        val date = video.released?.take(10)
        val line = listOfNotNull(video.season?.let { s -> video.episode?.let { "S$s E$it" } }, date).joinToString(" · ")
        if (line.isNotEmpty()) Text(line, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
    }
}
