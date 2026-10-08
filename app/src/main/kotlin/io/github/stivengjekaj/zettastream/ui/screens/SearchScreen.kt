package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.stivengjekaj.zettastream.addon.MetaPreview
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Message
import io.github.stivengjekaj.zettastream.ui.components.PosterCard
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary

/** The last search, so that Back from a title shows the same results. */
private object LastSearch {
    var query = ""
    var results: List<MetaPreview> = emptyList()
}

@Composable
fun SearchScreen(app: AppState) {
    val tv = app.isTv
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var text by rememberSaveable { mutableStateOf(LastSearch.query) }
    var query by remember { mutableStateOf(LastSearch.query) }
    var results by remember { mutableStateOf(LastSearch.results) }
    var searching by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank() || query == LastSearch.query && results.isNotEmpty()) return@LaunchedEffect
        searching = true
        results = emptyList()
        app.container.addons.search(query).collect { results = it }
        LastSearch.query = query
        LastSearch.results = results
        searching = false
    }

    Column(Modifier.fillMaxSize().padding(top = if (tv) 32.dp else 16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            // A placeholder, not a floating label: a label cuts a gap in the top border.
            placeholder = { Text("Search titles", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextSecondary) },
            shape = Corner,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { query = text.trim(); keyboard?.hide() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent, unfocusedBorderColor = Outline,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = Accent,
            ),
            // The field draws its own border when it has the focus, so it has no focus ring.
            // A text field keeps the Down key, so send it on to the results.
            modifier = Modifier.fillMaxWidth().padding(horizontal = Sizes.gutter(tv)).onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
                    keyboard?.hide()
                    focusManager.moveFocus(FocusDirection.Down)
                } else {
                    false
                }
            },
        )
        if (searching) LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth().padding(horizontal = Sizes.gutter(tv), vertical = 8.dp))
        if (!searching && query.isNotBlank() && results.isEmpty()) {
            Message("Nothing found", "No catalog with search found \"$query\".", tv)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Sizes.posterWidth(tv) + 8.dp),
            contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(results, key = { it.type + it.id }) { meta ->
                PosterCard(
                    meta, Sizes.posterWidth(tv), onClick = { app.open(Screen.Detail(meta)) }, onFocus = { app.focusedMeta = meta },
                    focusKey = "search|${meta.type}|${meta.id}",
                )
            }
        }
    }
}
