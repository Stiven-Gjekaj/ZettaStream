package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.stivengjekaj.zettastream.profile.ProfileStore
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.FocusMemory
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Background
import io.github.stivengjekaj.zettastream.ui.theme.Outline
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** A round picture of a profile. With no picture, it shows the first letter of the name. */
@Composable
fun ProfileCircle(name: String, avatar: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(SurfaceHigh), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(), color = TextPrimary, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold)
        if (avatar != null) AsyncImage(avatar, name, contentScale = ContentScale.Crop, alignment = Alignment.TopCenter, modifier = Modifier.fillMaxSize())
    }
}

/** "Who is watching?", as on Netflix. The app starts here, and the icon bar opens it again. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilesScreen(app: AppState) {
    val c = app.container
    val tv = app.isTv
    val data by c.profiles.profiles.collectAsState()
    var editing by rememberSaveable { mutableStateOf(false) }
    val lastFocus = remember { FocusRequester() }
    val size = if (tv) 132.dp else 96.dp

    fun choose(id: String) {
        if (editing) { app.open(Screen.EditProfile(id)); return }
        c.scope.launch {
            c.selectProfile(id)
            // The search and the focus of the screens belong to the profile before.
            LastSearch.query = ""
            LastSearch.results = emptyList()
            FocusMemory.pending = null
            app.homeFilter = null
            app.open(Screen.Home)
        }
    }

    // The profile of the last time has the focus, so OK starts it at once.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { lastFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (editing) "Edit profiles" else "Who is watching?",
            color = TextPrimary, fontSize = if (tv) 44.sp else 30.sp, fontWeight = FontWeight.ExtraBold,
        )
        FlowRow(
            Modifier.padding(vertical = if (tv) 48.dp else 32.dp),
            horizontalArrangement = Arrangement.spacedBy(if (tv) 36.dp else 20.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            data.profiles.forEach { profile ->
                val focus = if (profile.id == (data.last ?: data.profiles.first().id)) Modifier.focusRequester(lastFocus) else Modifier
                ProfileTile(profile.name, size, onClick = { choose(profile.id) }, modifier = focus) {
                    ProfileCircle(profile.name, profile.avatar, size)
                    if (editing) {
                        Box(Modifier.size(size).clip(CircleShape).background(Background.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Edit, "Edit", tint = TextPrimary, modifier = Modifier.size(size / 3))
                        }
                    }
                }
            }
            if (data.profiles.size < ProfileStore.MAX_PROFILES) {
                ProfileTile("Add profile", size, onClick = { app.open(Screen.EditProfile(null)) }) {
                    Box(Modifier.size(size).clip(CircleShape).border(2.dp, Outline, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = TextSecondary, modifier = Modifier.size(size / 3))
                    }
                }
            }
        }
        ZButton(onClick = { editing = !editing }, tv = tv) { Text(if (editing) "Done" else "Edit profiles") }
    }
}

@Composable
private fun ProfileTile(label: String, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier, picture: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size + 24.dp)) {
        Box(
            modifier.size(size).focusRing(CircleShape, scaleTo = 1.1f).clip(CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { picture() }
        Text(
            label, color = TextSecondary, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}
