package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.ui.components.HeroBanner
import io.github.stivengjekaj.zettastream.update.UpdateState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.BuildConfig
import io.github.stivengjekaj.zettastream.addon.CatalogRow
import io.github.stivengjekaj.zettastream.addon.HomeRows
import io.github.stivengjekaj.zettastream.library.Library
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Message
import io.github.stivengjekaj.zettastream.ui.components.PlaceholderRow
import io.github.stivengjekaj.zettastream.ui.components.PosterCard
import io.github.stivengjekaj.zettastream.ui.components.PosterRow
import io.github.stivengjekaj.zettastream.ui.components.SectionTitle
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.ZChip
import io.github.stivengjekaj.zettastream.ui.components.TvRow
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Surface
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.ui.typeKey
import io.github.stivengjekaj.zettastream.ui.typeLabel
import io.github.stivengjekaj.zettastream.ui.typeKey
import io.github.stivengjekaj.zettastream.update.UpdateChecker

@Composable
fun HomeScreen(app: AppState) {
    val c = app.container
    val tv = app.isTv
    val addonState by c.addons.addons.collectAsState()
    val sources by c.sources.sources.collectAsState()
    val library by c.library.data.collectAsState()
    val rows = remember(addonState) { c.addons.homeRows() }
    val types = remember(addonState) { c.addons.types() }
    val viewer by c.settings.settings.collectAsState()
    val shown = rows.filter { (app.homeFilter == null || typeKey(it.catalog.type) == app.homeFilter) && it.key !in viewer.hiddenRows }
    val resume = remember(library) { Library.continueWatching(library) }
    val latest = app.latestVersion
    // The banner shows the first titles with art from the first row.
    val firstRow = shown.firstOrNull()
    // A cached row shows at once. Then, after Back, the list has the same items as before and keeps its place.
    fun pick(metas: List<MetaPreview>) = metas.filter { it.background != null }.take(6)
    val cachedFeatured = remember(firstRow?.key) { firstRow?.let { c.addons.cachedCatalog(it) }?.let(::pick).orEmpty() }
    val featured by produceState(cachedFeatured, firstRow?.key) {
        // A new first row, for example after a filter, starts from its own cached titles.
        value = cachedFeatured
        val row = firstRow ?: return@produceState
        if (value.isEmpty()) value = pick(runCatching { c.addons.catalog(row) }.getOrDefault(emptyList()))
    }

    // The banner takes the focus one time only. After Back, the focus returns to the item that had it.
    var bannerFocused by rememberSaveable { mutableStateOf(false) }
    // Home shows when the art of the banner is there, so that the screen does not jump. A slow or
    // missing picture waits COVER_WAIT at most.
    var coverReady by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(COVER_WAIT); coverReady = true }
    if (sources.isEmpty() || (!addonState.loading && rows.isEmpty())) coverReady = true
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()

    if (!coverReady) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) }
    }
    LazyColumn(
        Modifier.fillMaxSize().alpha(if (coverReady) 1f else 0f),
        state = list,
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        if (featured.isNotEmpty()) {
            item(key = "banner") {
                HeroBanner(
                    featured, tv,
                    inWatchlist = { meta -> library.watchlist.any { it.id == meta.id } },
                    // A film plays at once. A series opens its page, where the viewer chooses the episode.
                    onPlay = { meta ->
                        if (typeKey(meta.type) == "movie") app.open(Screen.AutoPlay(meta, meta.id, meta.name, emptyList()))
                        else app.open(Screen.Detail(meta))
                    },
                    onDetails = { app.open(Screen.Detail(it)) },
                    onWatchlist = { app.toggleWatchlist(it) },
                    takeFocus = !bannerFocused,
                    onFocusTaken = { bannerFocused = true },
                    // The whole banner shows when one of its buttons has the focus, not only its lower part.
                    onFocused = { scope.launch { withFrameNanos { }; list.animateScrollToItem(0) } },
                    onCoverLoaded = { coverReady = true },
                )
            }
        } else {
            item {
                Row(
                    Modifier.padding(start = Sizes.gutter(tv), end = Sizes.gutter(tv), top = if (tv) 32.dp else 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Zetta", color = Accent, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold)
                    Text("Stream", color = TextSecondary, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (latest != null && UpdateChecker.isNewer(latest, BuildConfig.VERSION_NAME)) {
            item { UpdateBanner(app, latest) }
        }
        if (types.size > 1) {
            item {
                TvRow(tv) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(listOf<String?>(null) + types) { type ->
                            ZChip(typeLabel(type), selected = app.homeFilter == type, tv = tv, onClick = { app.homeFilter = type })
                        }
                    }
                }
            }
        }
        if (sources.isEmpty()) {
            item {
                Column {
                    Message(
                        "No sources yet",
                        "ZettaStream comes with no sources. Add your Stremio addons and IPTV playlists in Settings, Sources. " +
                            "On a TV, you scan a QR code with your phone and paste the list there.",
                        tv,
                    )
                    ZButton(
                        onClick = { app.open(Screen.Sources) },
                        tv = tv,
                        primary = true,
                        modifier = Modifier.padding(horizontal = Sizes.gutter(tv)),
                    ) { Text("Add sources") }
                }
            }
        } else if (!addonState.loading && rows.isEmpty() && addonState.addons.isEmpty()) {
            item {
                Column {
                    Message("No addon answered", "Check the network, or check the list in Settings, Sources.", tv)
                    ZButton(
                        onClick = { c.addons.retry() },
                        tv = tv,
                        primary = true,
                        modifier = Modifier.padding(horizontal = Sizes.gutter(tv)),
                    ) { Text("Try again") }
                }
            }
        }
        if (resume.isNotEmpty() && (app.homeFilter == null)) {
            item(key = "resume") {
                SectionTitle("Continue watching", tv)
                TvRow(tv) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (tv) 18.dp else 12.dp),
                    ) {
                        items(resume, key = { it.videoId }) { p ->
                            PosterCard(
                                p.meta, Sizes.posterWidth(tv),
                                onClick = { app.open(Screen.Detail(p.meta)) },
                                onFocus = { app.focusedMeta = p.meta },
                                progress = if (p.duration > 0) p.position.toFloat() / p.duration else null,
                                caption = p.label,
                                focusKey = "resume|${p.videoId}",
                            )
                        }
                    }
                }
            }
        }
        // The watchlist has its own row under Continue watching. The type filter applies to it too.
        val watchlist = library.watchlist.filter { app.homeFilter == null || typeKey(it.type) == app.homeFilter }
        if (watchlist.isNotEmpty()) {
            item(key = "watchlist") {
                SectionTitle("Your watchlist", tv, detail = count(watchlist.size, "title"))
                PosterRow(watchlist, tv, onClick = { app.open(Screen.Detail(it)) }, onFocus = { app.focusedMeta = it }, keyPrefix = "home-watchlist")
            }
        }
        if (addonState.loading && rows.isEmpty() && sources.isNotEmpty()) {
            item { PlaceholderRow(tv) }
        }
        itemsIndexed(shown, key = { _, row -> row.key }) { index, row -> CatalogRowView(app, row, shown.take(index)) }
    }
}

@Composable
private fun CatalogRowView(app: AppState, row: CatalogRow, above: List<CatalogRow>) {
    val tv = app.isTv
    val cached = app.container.addons.cachedCatalog(row)
    val metas by produceState(cached, row.key) {
        if (cached == null) value = runCatching { app.container.addons.catalog(row) }.getOrDefault(emptyList())
    }
    val list = metas
    if (list != null && list.isEmpty()) return
    // A row that mostly repeats a row above does not show.
    if (list != null && HomeRows.isRepeat(list.map { it.id }, above.mapNotNull { r -> app.container.addons.cachedCatalog(r)?.map { it.id } })) return
    SectionTitle(row.title, tv, detail = "${typeLabel(row.catalog.type)} from ${row.addon.name}")
    if (list == null) PlaceholderRow(tv)
    else PosterRow(list, tv, onClick = { app.open(Screen.Detail(it)) }, onFocus = { app.focusedMeta = it }, keyPrefix = row.key)
}

/** Tells that a new version is out, and installs it with one button. */
@Composable
fun UpdateBanner(app: AppState, latest: String) {
    val tv = app.isTv
    val installer = app.container.installer
    val state by installer.updates.collectAsState()
    // The message has its own side space. The button takes the same space.
    Column {
        Message("Version $latest is available", updateText(state), tv)
        ZButton(
            onClick = { installer.install() },
            tv = tv,
            modifier = Modifier.padding(start = Sizes.gutter(tv), bottom = 8.dp),
        ) { Text(if (state is UpdateState.NeedsPermission) "Allowed? Install now" else "Install the update") }
    }
}

/** The text for each step of an update. */
fun updateText(state: UpdateState): String = when (state) {
    UpdateState.Idle -> "Install it here, or open Downloader and enter the code ${UpdateChecker.DOWNLOADER_CODE}"
    is UpdateState.Downloading -> "Downloading: ${state.percent}%"
    UpdateState.Installing -> "Android asks you to confirm the update"
    UpdateState.NeedsPermission -> "Allow ZettaStream to install apps in the screen that opened, then press Back and install again"
    is UpdateState.Failed -> "The update did not install (${state.reason}). Try again, or use Downloader with the code ${UpdateChecker.DOWNLOADER_CODE}"
}

/** The longest wait for the art of the banner before Home shows. */
private const val COVER_WAIT = 6_000L
