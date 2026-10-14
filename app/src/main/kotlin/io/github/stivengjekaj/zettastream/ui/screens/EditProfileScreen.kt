package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.profile.Avatar
import io.github.stivengjekaj.zettastream.profile.ProfileStore
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.TvRow
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** Adds a profile, or changes the name and the picture of one. The pictures come from AniList and TVmaze. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditProfileScreen(app: AppState, id: String?) {
    val c = app.container
    val tv = app.isTv
    val data by c.profiles.profiles.collectAsState()
    val profile = data.profiles.firstOrNull { it.id == id }
    var name by rememberSaveable { mutableStateOf(profile?.name.orEmpty()) }
    var avatar by rememberSaveable { mutableStateOf(profile?.avatar) }
    var search by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var sections by remember { mutableStateOf(emptyList<Pair<String, List<Avatar>>>()) }
    val save = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(query) {
        loading = true
        sections = if (query.isBlank()) c.avatars.featured() else c.avatars.search(query)
        loading = false
    }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { save.requestFocus() } }

    fun done() {
        c.scope.launch {
            if (profile == null) c.profiles.add(name, avatar) else c.profiles.edit(profile.id, name, avatar)
            app.back {}
        }
    }

    // A text field keeps the Up and Down keys, and on a TV the keyboard opens when the field gets the focus.
    // Until the viewer presses OK in a field, Up and Down move on and the keyboard stays closed. This runs
    // before the keyboard, which would take the keys.
    var typing by remember { mutableStateOf<String?>(null) }
    fun field(name: String) = Modifier
        .onFocusChanged { if (!it.isFocused && typing == name) typing = null }
        .onInterceptKeyBeforeSoftKeyboard {
            if (it.type != KeyEventType.KeyDown || typing == name) return@onInterceptKeyBeforeSoftKeyboard false
            when (it.key) {
                Key.DirectionCenter, Key.Enter -> { typing = name; keyboard?.show(); true }
                Key.DirectionDown -> { keyboard?.hide(); focusManager.moveFocus(FocusDirection.Down); true }
                Key.DirectionUp -> { keyboard?.hide(); focusManager.moveFocus(FocusDirection.Up); true }
                else -> false
            }
        }
        .onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (it.key) {
                Key.DirectionDown -> { keyboard?.hide(); typing = null; focusManager.moveFocus(FocusDirection.Down) }
                Key.DirectionUp -> { keyboard?.hide(); typing = null; focusManager.moveFocus(FocusDirection.Up) }
                else -> false
            }
        }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Accent, unfocusedBorderColor = Outline, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, cursorColor = Accent,
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = if (tv) 40.dp else 20.dp)) {
        item {
            Row(Modifier.padding(horizontal = Sizes.gutter(tv)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                ProfileCircle(name.ifBlank { "?" }, avatar, if (tv) 140.dp else 96.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (profile == null) "Add a profile" else "Edit profile", color = TextPrimary, fontSize = if (tv) 32.sp else 24.sp, fontWeight = FontWeight.ExtraBold)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it.take(ProfileStore.MAX_NAME) }, singleLine = true, shape = Corner,
                        placeholder = { Text("Name", color = TextSecondary) }, colors = fieldColors,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); typing = null }),
                        modifier = Modifier.fillMaxWidth(if (tv) 0.6f else 1f).then(field("name")),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ZButton(onClick = ::done, tv = tv, primary = true, modifier = Modifier.focusRequester(save)) { Text("Save") }
                        ZButton(onClick = { app.back {} }, tv = tv) { Text("Cancel") }
                        if (profile != null && data.profiles.size > 1) {
                            ZButton(onClick = { c.scope.launch { c.profiles.delete(profile.id); app.back {} } }, tv = tv) { Text("Delete profile") }
                        }
                        if (avatar != null) ZButton(onClick = { avatar = null }, tv = tv) { Text("No picture") }
                    }
                }
            }
        }
        item {
            Text(
                "Choose a picture", color = TextPrimary, fontSize = if (tv) 22.sp else 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Sizes.gutter(tv), end = Sizes.gutter(tv), top = 32.dp, bottom = 8.dp),
            )
            OutlinedTextField(
                value = search, onValueChange = { search = it }, singleLine = true, shape = Corner, colors = fieldColors,
                placeholder = { Text("Search a character or an actor", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.padding(start = 14.dp, end = 6.dp)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { query = search.trim(); keyboard?.hide(); typing = null }),
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv)).fillMaxWidth(if (tv) 0.6f else 1f).then(field("search")),
            )
        }
        if (loading) {
            item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) } }
        } else if (sections.isEmpty()) {
            item {
                Text(
                    if (query.isBlank()) "The pictures did not load. Check the network." else "No picture found for \"$query\".",
                    color = TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(horizontal = Sizes.gutter(tv), vertical = 24.dp),
                )
            }
        }
        sections.forEach { (title, avatars) ->
            item(key = "section-$title-$query") {
                Text(
                    title, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = Sizes.gutter(tv), top = 20.dp, bottom = 4.dp),
                )
                TvRow(tv) {
                    LazyRow(contentPadding = PaddingValues(horizontal = Sizes.gutter(tv), vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        items(avatars, key = { it.url }) { choice ->
                            AvatarChoice(choice, selected = choice.url == avatar, tv = tv) { avatar = choice.url }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarChoice(avatar: Avatar, selected: Boolean, tv: Boolean, onClick: () -> Unit) {
    val size = if (tv) 96.dp else 72.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size + 24.dp)) {
        Box(Modifier.size(size).focusRing(CircleShape, scaleTo = 1.1f).clip(CircleShape).clickable(onClick = onClick)) {
            ProfileCircle(avatar.name, avatar.url, size)
            // The chosen picture has a thick white ring.
            if (selected) Box(Modifier.size(size).border(4.dp, Accent, CircleShape))
        }
        Text(
            avatar.name, color = if (selected) TextPrimary else TextSecondary, fontSize = 12.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp),
        )
    }
}
