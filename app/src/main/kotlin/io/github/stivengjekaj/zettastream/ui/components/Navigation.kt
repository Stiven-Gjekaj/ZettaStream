package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import io.github.stivengjekaj.zettastream.ui.theme.OnAccent
import io.github.stivengjekaj.zettastream.ui.AppState
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusGroup
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Background
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary

data class NavItem(val screen: Screen, val label: String, val icon: ImageVector, val key: String)

val NavItems = listOf(
    NavItem(Screen.Home, "Home", Icons.Rounded.Home, "1"),
    NavItem(Screen.Search, "Search", Icons.Rounded.Search, "2"),
    NavItem(Screen.Library, "Library", Icons.Rounded.VideoLibrary, "3"),
    NavItem(Screen.Settings, "Settings", Icons.Rounded.Settings, "4"),
    NavItem(Screen.Live, "Live TV", Icons.Rounded.LiveTv, "5"),
)

/** The width of the icon bar. The screens on a TV start after it. */
val RailWidth = 76.dp
private val RailOpenWidth = 240.dp

/**
 * The icon bar on the left of a TV screen, after Miruro 2.0. It shows the
 * labels while one of its items has the focus. Left goes into it only with a
 * new press (see [io.github.stivengjekaj.zettastream.remote.EdgeGuard]).
 * Right, Back, Menu, and 0 go back to the item that had the focus.
 */
@Composable
fun TvRail(app: AppState, current: Screen, modifier: Modifier = Modifier) {
    val open = app.railFocused
    val items = remember { List(NavItems.size) { FocusRequester() } }
    val selectedIndex = NavItems.indexOfFirst { it.screen == current }.coerceAtLeast(0)
    app.railEntry = items[selectedIndex]
    val width by animateDpAsState(if (open) RailOpenWidth else RailWidth, label = "rail")
    Column(
        modifier
            .fillMaxHeight()
            .width(width)
            .background(
                if (open) Brush.horizontalGradient(listOf(Background, Background.copy(alpha = 0.96f)))
                else Brush.horizontalGradient(listOf(Background, Background)),
            )
            .onFocusChanged { app.railFocused = it.hasFocus }
            .focusProperties {
                // Left from a row goes in only with a new press, to the item of the screen that is open.
                // Any other way in, such as the first key press after the start, goes to the screen.
                onEnter = {
                    when {
                        app.railRequested -> Unit
                        requestedFocusDirection != FocusDirection.Left -> app.content.requestFocus()
                        app.edgeGuard.allowed -> items[selectedIndex].requestFocus()
                        else -> cancelFocusChange()
                    }
                }
            }
            .focusGroup()
            // Right goes back to the item of the screen that had the focus, or to the screen when that item is gone.
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight) { app.leaveRail(); true } else false
            }
            .padding(vertical = 24.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Z", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 14.dp, bottom = 20.dp))
        // Settings sits at the bottom. Up and Down follow the order on the screen.
        val (bottom, top) = NavItems.indices.partition { NavItems[it].screen == Screen.Settings }
        val shown = top + bottom
        (top + listOf(null) + bottom).forEach { entry ->
            if (entry == null) { Spacer(Modifier.weight(1f)); return@forEach }
            val index = entry
            val item = NavItems[index]
            val place = shown.indexOf(index)
            val active = item.screen == current
            Row(
                Modifier
                    .fillMaxWidth()
                    .focusRequester(items[index])
                    .focusProperties {
                        up = if (place > 0) items[shown[place - 1]] else FocusRequester.Cancel
                        down = if (place < shown.lastIndex) items[shown[place + 1]] else FocusRequester.Cancel
                    }
                    .focusRing(Corner, scaleTo = 1.04f, rememberForReturn = false)
                    .clip(Corner)
                    .background(if (active) Accent else Color.Transparent)
                    .clickable { app.open(item.screen) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.icon, item.label, tint = if (active) OnAccent else TextSecondary, modifier = Modifier.size(24.dp))
                if (open) {
                    Spacer(Modifier.width(16.dp))
                    Text(item.label, color = if (active) OnAccent else TextPrimary, fontSize = 18.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Text(item.key, color = if (active) OnAccent.copy(alpha = 0.6f) else TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

/** The tab bar at the bottom of a phone. */
@Composable
fun PhoneTabBar(current: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar(containerColor = Background) {
        NavItems.forEach { item ->
            NavigationBarItem(
                selected = item.screen == current,
                onClick = { onSelect(item.screen) },
                icon = { Icon(item.icon, item.label) },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = TextPrimary,
                    indicatorColor = SurfaceHigh,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                ),
            )
        }
    }
}

@Composable
fun Toast(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(text != null, modifier = modifier, enter = fadeIn(), exit = fadeOut()) {
        Text(
            text.orEmpty(),
            color = TextPrimary,
            fontSize = 16.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceHigh)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

