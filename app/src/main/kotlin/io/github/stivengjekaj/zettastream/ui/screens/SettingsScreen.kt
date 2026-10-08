package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.stivengjekaj.zettastream.BuildConfig
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.update.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun SettingsRow(title: String, detail: String, tv: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Sizes.gutter(tv), vertical = 5.dp)
            .focusRing(shape, scaleTo = 1.02f)
            .clip(shape)
            .background(SurfaceHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(title, color = TextPrimary, fontSize = if (tv) 19.sp else 16.sp, fontWeight = FontWeight.SemiBold)
        Text(detail, color = TextSecondary, fontSize = 14.sp)
    }
}

@Composable
fun SettingsScreen(app: AppState) {
    val c = app.container
    val tv = app.isTv
    val scope = rememberCoroutineScope()
    val sources by c.sources.sources.collectAsState()
    val addons by c.addons.addons.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = if (tv) 32.dp else 16.dp)) {
        item {
            Text("Settings", color = TextPrimary, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv), vertical = 8.dp))
        }
        item {
            SettingsRow(
                "Sources",
                "${sources.size} sources, ${addons.addons.size} addons work" +
                    (if (addons.failures.isNotEmpty()) ", ${addons.failures.size} do not answer" else ""),
                tv,
            ) { app.open(Screen.Sources) }
        }
        item { SettingsRow("Remote control test", "Shows the code of each button that you press", tv) { app.open(Screen.KeyTest) } }
        item { SettingsRow("Clear the watch history", "Removes the progress of each video. The watchlist stays.", tv) { confirmClear = true } }
        item {
            SettingsRow("Check for updates", update ?: "Version ${BuildConfig.VERSION_NAME}", tv) {
                update = "Checking"
                scope.launch {
                    val latest = c.updates.latestVersion()
                    app.latestVersion = latest
                    update = when {
                        latest == null -> "GitHub did not answer. Try again later."
                        UpdateChecker.isNewer(latest, BuildConfig.VERSION_NAME) -> "Version $latest is available. Open Downloader and enter ${UpdateChecker.DOWNLOAD_URL}"
                        else -> "Version ${BuildConfig.VERSION_NAME} is the newest"
                    }
                }
            }
        }
        item {
            Text(
                "ZettaStream ${BuildConfig.VERSION_NAME}. It contains no content and no sources. " +
                    "You add your own sources, and you are responsible for them.\nhttps://github.com/Stiven-Gjekaj/ZettaStream",
                color = TextSecondary, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv), vertical = 16.dp),
            )
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the watch history?") },
            text = { Text("The progress of each video goes. The watchlist stays.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { c.library.clearHistory() }; confirmClear = false }, modifier = Modifier.focusRing()) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }, modifier = Modifier.focusRing()) { Text("Cancel") } },
        )
    }
}
