package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.BuildConfig
import io.github.stivengjekaj.zettastream.addon.CatalogRow
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.library.Library
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Message
import io.github.stivengjekaj.zettastream.ui.components.PlaceholderRow
import io.github.stivengjekaj.zettastream.ui.components.PosterCard
import io.github.stivengjekaj.zettastream.ui.components.PosterRow
import io.github.stivengjekaj.zettastream.ui.components.SectionTitle
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Cyan
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.ui.typeLabel
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
    val shown = rows.filter { app.homeFilter == null || it.catalog.type == app.homeFilter }
    val resume = remember(library) { Library.continueWatching(library) }
    val latest = app.latestVersion

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
        item {
            Row(
                Modifier.padding(start = Sizes.gutter(tv), end = Sizes.gutter(tv), top = if (tv) 32.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Zetta", color = Accent, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold)
                Text("Stream", color = Cyan, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (latest != null && UpdateChecker.isNewer(latest, BuildConfig.VERSION_NAME)) {
            item {
                Message(
                    "Version $latest is available",
                    "Open Downloader and enter ${UpdateChecker.DOWNLOAD_URL}",
                    tv,
                )
            }
        }
        if (types.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(listOf<String?>(null) + types) { type ->
                        FilterChip(
                            selected = app.homeFilter == type,
                            onClick = { app.homeFilter = type },
                            label = { Text(typeLabel(type), fontSize = if (tv) 16.sp else 14.sp) },
                            modifier = Modifier.focusRing(scaleTo = 1.04f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent, selectedLabelColor = TextPrimary,
                                containerColor = SurfaceHigh, labelColor = TextSecondary,
                            ),
                        )
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
                    Button(
                        onClick = { app.open(Screen.Sources) },
                        modifier = Modifier.padding(horizontal = Sizes.gutter(tv)).focusRing(),
                    ) { Text("Add sources") }
                }
            }
        } else if (!addonState.loading && rows.isEmpty() && addonState.addons.isEmpty()) {
            item { Message("No addon answered", "Check the list in Settings, Sources.", tv) }
        }
        if (resume.isNotEmpty() && (app.homeFilter == null)) {
            item(key = "resume") {
                SectionTitle("Continue watching", tv)
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
                        )
                    }
                }
            }
        }
        if (addonState.loading && rows.isEmpty() && sources.isNotEmpty()) {
            item { PlaceholderRow(tv) }
        }
        items(shown, key = { it.key }) { row -> CatalogRowView(app, row) }
    }
}

@Composable
private fun CatalogRowView(app: AppState, row: CatalogRow) {
    val tv = app.isTv
    val metas by produceState<List<MetaPreview>?>(null, row.key) {
        value = runCatching { app.container.addons.catalog(row) }.getOrDefault(emptyList())
    }
    val list = metas
    if (list != null && list.isEmpty()) return
    SectionTitle(row.catalog.name, tv, detail = "${typeLabel(row.catalog.type)} from ${row.addon.name}")
    if (list == null) PlaceholderRow(tv)
    else PosterRow(list, tv, onClick = { app.open(Screen.Detail(it)) }, onFocus = { app.focusedMeta = it })
}
