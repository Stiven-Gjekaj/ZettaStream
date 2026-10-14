package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.Icons
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.itemsIndexed
import io.github.stivengjekaj.zettastream.addon.HomeRows
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.components.PageTitle
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.ui.typeLabel
import kotlinx.coroutines.launch

/** Lists each row that the addons give, so that the viewer can turn rows off and on. */
@Composable
fun HomeRowsScreen(app: AppState) {
    val c = app.container
    val tv = app.isTv
    val viewer by c.settings.settings.collectAsState()
    val addons by c.addons.addons.collectAsState()
    val rows = remember(addons, viewer.rowOrder) { c.addons.homeRows(viewer.rowOrder) }
    // The arrow that the viewer pressed keeps the focus after its row moves.
    val arrows = remember { mutableMapOf<String, FocusRequester>() }
    var follow by remember { mutableStateOf<String?>(null) }
    fun arrow(key: String) = arrows.getOrPut(key) { FocusRequester() }
    fun move(index: Int, step: Int) {
        val order = HomeRows.move(rows, index, step)
        follow = "${rows[index].key}|$step"
        c.scope.launch { c.settings.update { it.copy(rowOrder = order) } }
    }
    LaunchedEffect(rows) {
        val target = follow ?: return@LaunchedEffect
        follow = null
        withFrameNanos { }
        runCatching { arrow(target).requestFocus() }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = if (tv) 32.dp else 16.dp)) {
        item {
            PageTitle("Home rows", tv)
            Text(
                "Press OK to turn a row off or on. The arrows on the right move a row up or down. A row that repeats a row above it does not show.",
                color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = Sizes.gutter(tv), vertical = 8.dp),
            )
        }
        itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
            val on = row.key !in viewer.hiddenRows
            Row(Modifier.padding(end = Sizes.gutter(tv)), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    SettingsRow(row.title + if (on) "" else "  (off)", "${typeLabel(row.catalog.type)} from ${row.addon.name}", tv) {
                        c.scope.launch {
                            c.settings.update { s -> s.copy(hiddenRows = if (on) s.hiddenRows + row.key else s.hiddenRows - row.key) }
                        }
                    }
                }
                ZButton(onClick = { move(index, -1) }, tv = tv, modifier = Modifier.focusRequester(arrow("${row.key}|-1"))) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                Spacer(Modifier.width(8.dp))
                ZButton(onClick = { move(index, 1) }, tv = tv, modifier = Modifier.focusRequester(arrow("${row.key}|1"))) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
            }
        }
    }
}
