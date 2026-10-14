package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.addon.CatalogPaging
import io.github.stivengjekaj.zettastream.addon.CatalogRow
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Message
import io.github.stivengjekaj.zettastream.ui.components.PageTitle
import io.github.stivengjekaj.zettastream.ui.components.PosterCard
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.ui.typeLabel

/** The pages of titles that each catalog loaded, so that Back from a title shows the same grid. */
private object LoadedPages {
    val titles = mutableMapOf<String, Pair<List<MetaPreview>, Boolean>>()
}

/**
 * All the titles of one catalog in a grid, as in Search. Near the end of the grid, the
 * next page loads, for as long as the addon gives new titles.
 */
@Composable
fun CatalogScreen(app: AppState, row: CatalogRow) {
    val c = app.container
    val tv = app.isTv
    val start = LoadedPages.titles[row.key]
    var titles by remember(row.key) { mutableStateOf(start?.first ?: c.addons.cachedCatalog(row).orEmpty()) }
    var more by remember(row.key) { mutableStateOf(start?.second ?: CatalogPaging.canPage(row)) }
    var loading by remember(row.key) { mutableStateOf(false) }
    var failed by remember(row.key) { mutableStateOf(false) }
    val grid = rememberLazyGridState()

    suspend fun loadNext() {
        if (loading) return
        loading = true
        val page = runCatching { c.addons.catalog(CatalogPaging.page(row, titles.size)) }
        loading = false
        page.onSuccess { list ->
            val (next, fresh) = CatalogPaging.add(titles, list)
            titles = next
            more = fresh && CatalogPaging.canPage(row)
        }.onFailure { failed = true; more = false }
        LoadedPages.titles[row.key] = titles to more
    }

    // The first page, when the row has not loaded it yet.
    LaunchedEffect(row.key) { if (titles.isEmpty()) loadNext() }
    // The next page, when the viewer comes near the end of the grid.
    LaunchedEffect(row.key, grid) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (more && !loading && last >= titles.size - PRELOAD) loadNext()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(Sizes.posterWidth(tv) + 8.dp),
        state = grid,
        contentPadding = PaddingValues(start = Sizes.gutter(tv), end = Sizes.gutter(tv), top = if (tv) 32.dp else 16.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(row.title, color = io.github.stivengjekaj.zettastream.ui.theme.TextPrimary, fontSize = if (tv) 36.sp else 26.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                Text("${typeLabel(row.catalog.type)} from ${row.addon.name}", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        items(titles, key = { it.type + it.id }) { meta ->
            PosterCard(
                meta, Sizes.posterWidth(tv), onClick = { app.open(Screen.Detail(meta)) }, onFocus = { app.focusedMeta = meta },
                focusKey = "catalog|${row.key}|${meta.type}|${meta.id}",
            )
        }
        if (loading) {
            item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) }
        } else if (titles.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Message(if (failed) "The addon did not answer" else "No titles", if (failed) "Press Back and try again later." else "This catalog gives no titles.", tv)
            }
        }
    }
}

/** The next page loads when this many titles are left below the last one on the screen. */
private const val PRELOAD = 12
