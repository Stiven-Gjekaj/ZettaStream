package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import io.github.stivengjekaj.zettastream.ui.FocusMemory
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.rounded.History
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import io.github.stivengjekaj.zettastream.settings.SearchHistory
import androidx.compose.ui.ExperimentalComposeUiApi
import io.github.stivengjekaj.zettastream.remote.RemoteKeys
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.remote.RemoteAction
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.rounded.Mic
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.speech.RecognizerIntent
import android.content.Intent
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
import androidx.compose.ui.input.key.onInterceptKeyBeforeSoftKeyboard
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
import io.github.stivengjekaj.zettastream.ui.components.PageTitle
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary

/** The last search, so that Back from a title shows the same results. */
internal object LastSearch {
    var query = ""
    var results: List<MetaPreview> = emptyList()
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(app: AppState) {
    val tv = app.isTv
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var text by rememberSaveable { mutableStateOf(LastSearch.query) }
    var query by remember { mutableStateOf(LastSearch.query) }
    var results by remember { mutableStateOf(LastSearch.results) }
    var searching by remember { mutableStateOf(false) }
    var focusFirstResult by remember { mutableStateOf(false) }
    val viewer by app.container.settings.settings.collectAsState()
    val firstRecent = remember { FocusRequester() }
    val showRecent = query.isBlank() && viewer.searchHistory.isNotEmpty()

    LaunchedEffect(query) {
        if (query.isBlank() || query == LastSearch.query && results.isNotEmpty()) return@LaunchedEffect
        searching = true
        results = emptyList()
        app.container.addons.search(query).collect {
            // After a recent search, the first result takes the focus when it comes.
            if (focusFirstResult && results.isEmpty() && it.isNotEmpty()) {
                FocusMemory.pending = "search|${it.first().type}|${it.first().id}"
                focusFirstResult = false
            }
            results = it
        }
        val searched = query
        app.container.settings.update { it.copy(searchHistory = SearchHistory.add(it.searchHistory, searched)) }
        LastSearch.query = query
        LastSearch.results = results
        searching = false
    }

    // Voice search uses the speech service of the device. The remote control sends the words back as text.
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val said = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (!said.isNullOrEmpty()) { text = said; query = said }
    }
    fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a title")
        runCatching { voice.launch(intent) }.onFailure { app.toast = "Voice search is not available on this device" }
    }
    // The Search key of the remote control starts voice search on this screen.
    DisposableEffect(Unit) {
        app.screenActions = mapOf(RemoteAction.Search to { listen() })
        onDispose { app.screenActions = emptyMap() }
    }

    Column(Modifier.fillMaxSize().padding(top = if (tv) 32.dp else 16.dp)) {
        PageTitle("Search", tv, Modifier.padding(bottom = 8.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Sizes.gutter(tv)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                // A placeholder, not a floating label: a label cuts a gap in the top border.
                placeholder = { Text("Search titles", color = TextSecondary) },
                // On a TV, the app has no minimum touch size, so the icon needs its own space from the frame.
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.padding(start = 14.dp, end = 6.dp)) },
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
                modifier = Modifier.weight(1f)
                    // On a TV, a number key of the remote opens its screen, as on any other screen. This runs
                    // before the on-screen keyboard, which would type the digit. The keyboard itself still types
                    // digits, because its buttons do not send key presses.
                    .onInterceptKeyBeforeSoftKeyboard {
                        val native = it.nativeKeyEvent
                        if (!tv || native.keyCode !in NUMBER_KEYS) return@onInterceptKeyBeforeSoftKeyboard false
                        if (it.type == KeyEventType.KeyDown && RemoteKeys.global(native.keyCode) != RemoteAction.Search) {
                            keyboard?.hide()
                            app.onUnhandledKey(native)
                        }
                        true
                    }
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
                            keyboard?.hide()
                            // Down goes to the newest search, not to the button that is nearest below.
                            if (showRecent) runCatching { firstRecent.requestFocus() }.isSuccess
                            else focusManager.moveFocus(FocusDirection.Down)
                        } else {
                            false
                        }
                    },
            )
            ZButton(onClick = ::listen, tv = tv) {
                Icon(Icons.Rounded.Mic, null)
                Text("Voice", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (searching) LinearProgressIndicator(color = Accent, modifier = Modifier.fillMaxWidth().padding(horizontal = Sizes.gutter(tv), vertical = 8.dp))
        when {
            !searching && query.isNotBlank() && results.isEmpty() -> Message("Nothing found", "No catalog with search found \"$query\".", tv)
            showRecent -> Column(Modifier.padding(horizontal = Sizes.gutter(tv), vertical = 16.dp)) {
                Text("Recent searches", color = TextPrimary, fontSize = if (tv) 20.sp else 17.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    viewer.searchHistory.forEachIndexed { i, past ->
                        ZButton(onClick = { keyboard?.hide(); focusFirstResult = true; text = past; query = past }, tv = tv, modifier = if (i == 0) Modifier.focusRequester(firstRecent) else Modifier) {
                            Icon(Icons.Rounded.History, null, modifier = Modifier.size(18.dp))
                            Text(past, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                    ZButton(onClick = { app.container.scope.launch { app.container.settings.update { it.copy(searchHistory = emptyList()) } } }, tv = tv) {
                        Text("Clear", color = TextSecondary)
                    }
                }
            }
            query.isBlank() -> Message(
                "Find a film, a series, or an anime",
                if (tv) "Type a title and press Search. Or press the Search key of the remote and say the title." else "Type a title and press Search.",
                tv,
            )
            results.isNotEmpty() -> Text(
                "${count(results.size, "result")} for \"$query\"", color = TextSecondary, fontSize = 14.sp,
                modifier = Modifier.padding(start = Sizes.gutter(tv), end = Sizes.gutter(tv), top = 12.dp),
            )
        }
        // Better matches can come after the first answers. The grid keeps its place on the first card
        // that it showed, so while the viewer is not in the results, it goes back to the top.
        val grid = rememberLazyGridState()
        var inResults by remember { mutableStateOf(false) }
        LaunchedEffect(results.firstOrNull()?.id) { if (!inResults) grid.scrollToItem(0) }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(Sizes.posterWidth(tv) + 8.dp),
            state = grid,
            contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize().onFocusChanged { inResults = it.hasFocus },
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

/** The number keys of a remote control and of a keyboard. */
private val NUMBER_KEYS = (android.view.KeyEvent.KEYCODE_0..android.view.KeyEvent.KEYCODE_9) +
    (android.view.KeyEvent.KEYCODE_NUMPAD_0..android.view.KeyEvent.KEYCODE_NUMPAD_9)
