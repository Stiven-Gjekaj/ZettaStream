package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import io.github.stivengjekaj.zettastream.library.Library
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Message
import io.github.stivengjekaj.zettastream.ui.components.PosterCard
import io.github.stivengjekaj.zettastream.ui.components.PosterRow
import io.github.stivengjekaj.zettastream.ui.components.SectionTitle
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.TvRow
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary

@Composable
fun LibraryScreen(app: AppState) {
    val tv = app.isTv
    val data by app.container.library.data.collectAsState()
    val resume = remember(data) { Library.continueWatching(data) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = if (tv) 32.dp else 16.dp)) {
        item {
            Text("Library", color = TextPrimary, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv)))
        }
        if (resume.isEmpty() && data.watchlist.isEmpty()) {
            item {
                Message(
                    "Your library is empty",
                    if (tv) "Press the Red button on a title to add it to the watchlist. What you watch shows here too."
                    else "Add a title to the watchlist from its page. What you watch shows here too.",
                    tv,
                )
            }
        }
        if (resume.isNotEmpty()) {
            item {
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
                            )
                        }
                    }
                }
            }
        }
        if (data.watchlist.isNotEmpty()) {
            item {
                SectionTitle("Watchlist", tv)
                PosterRow(data.watchlist, tv, onClick = { app.open(Screen.Detail(it)) }, onFocus = { app.focusedMeta = it })
            }
        }
    }
}
