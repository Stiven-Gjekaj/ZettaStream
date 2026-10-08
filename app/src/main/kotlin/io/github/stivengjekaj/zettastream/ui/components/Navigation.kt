package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** The side menu on the TV. It is hidden until the user opens it. */
@Composable
fun TvSideMenu(open: Boolean, current: Screen, onSelect: (Screen) -> Unit) {
    AnimatedVisibility(open, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(listOf(Background.copy(alpha = 0.95f), Background.copy(alpha = 0.6f), Color.Transparent)),
            ),
        )
    }
    AnimatedVisibility(open, enter = slideInHorizontally { -it }, exit = slideOutHorizontally { -it }) {
        val selected = remember { FocusRequester() }
        val items = remember { List(NavItems.size) { FocusRequester() } }
        Column(
            Modifier
                .fillMaxHeight()
                .width(280.dp)
                .background(Background.copy(alpha = 0.98f))
                .padding(vertical = 48.dp, horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("ZettaStream", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 24.dp))
            NavItems.forEachIndexed { index, item ->
                val active = item.screen == current
                val shape = Corner
                Row(
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(items[index])
                        .then(if (active) Modifier.focusRequester(selected) else Modifier)
                        // The focus stays in the menu. Only Back, Menu, or 0 closes it.
                        .focusProperties {
                            up = if (index > 0) items[index - 1] else FocusRequester.Cancel
                            down = if (index < items.lastIndex) items[index + 1] else FocusRequester.Cancel
                            left = FocusRequester.Cancel
                            right = FocusRequester.Cancel
                        }
                        .focusRing(shape, scaleTo = 1.03f)
                        .clip(shape)
                        .background(if (active) SurfaceHigh else Color.Transparent)
                        .clickable { onSelect(item.screen) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, null, tint = if (active) Accent else TextSecondary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(item.label, color = TextPrimary, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    Text(item.key, color = TextSecondary, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Back, Menu, or 0: close", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp))
        }
        LaunchedEffect(Unit) { runCatching { selected.requestFocus() } }
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

