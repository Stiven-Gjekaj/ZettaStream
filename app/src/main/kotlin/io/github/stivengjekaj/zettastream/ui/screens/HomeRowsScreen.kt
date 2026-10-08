package io.github.stivengjekaj.zettastream.ui.screens

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
    val rows = remember(addons) { c.addons.homeRows() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = if (tv) 32.dp else 16.dp)) {
        item {
            Text("Home rows", color = TextPrimary, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv)))
            Text(
                "Press OK to turn a row off or on. New and trending rows come first. A row that repeats a row above it does not show.",
                color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(horizontal = Sizes.gutter(tv), vertical = 8.dp),
            )
        }
        items(rows, key = { it.key }) { row ->
            val on = row.key !in viewer.hiddenRows
            SettingsRow(row.title + if (on) "" else "  (off)", "${typeLabel(row.catalog.type)} from ${row.addon.name}", tv) {
                c.scope.launch {
                    c.settings.update { s -> s.copy(hiddenRows = if (on) s.hiddenRows + row.key else s.hiddenRows - row.key) }
                }
            }
        }
    }
}
