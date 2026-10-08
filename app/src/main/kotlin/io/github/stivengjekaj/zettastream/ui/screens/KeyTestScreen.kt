package io.github.stivengjekaj.zettastream.ui.screens

import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.remote.RemoteKeys
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary

/** Shows each key that reaches the app. Use it to confirm the buttons of a remote control. */
@Composable
fun KeyTestScreen(app: AppState) {
    val tv = app.isTv
    val events = remember { mutableStateListOf<String>() }
    DisposableEffect(Unit) {
        app.keyCapture = { event ->
            if (event.keyCode == KeyEvent.KEYCODE_BACK) false
            else {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    val global = RemoteKeys.global(event.keyCode)?.name ?: "-"
                    val player = RemoteKeys.player(event.keyCode)?.name ?: "-"
                    events.add(0, "%-28s code %3d  scan %3d   app: %-11s player: %s".format(
                        KeyEvent.keyCodeToString(event.keyCode), event.keyCode, event.scanCode, global, player))
                    if (events.size > 30) events.removeAt(events.lastIndex)
                }
                true
            }
        }
        onDispose { app.keyCapture = null }
    }
    Column(Modifier.fillMaxSize().padding(Sizes.gutter(tv))) {
        Text("Remote control test", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Press each button. A button that does not show here does not reach the app. Press Back to leave.",
            color = TextSecondary, fontSize = 16.sp, modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn {
            items(events) { Text(it, color = TextPrimary, fontSize = 15.sp, fontFamily = FontFamily.Monospace) }
        }
    }
}
