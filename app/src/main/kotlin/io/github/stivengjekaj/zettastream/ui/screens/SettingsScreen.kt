package io.github.stivengjekaj.zettastream.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import io.github.stivengjekaj.zettastream.settings.SettingsStore
import io.github.stivengjekaj.zettastream.settings.SubtitleBackground
import io.github.stivengjekaj.zettastream.settings.SubtitleColor
import io.github.stivengjekaj.zettastream.settings.SubtitleEdge
import io.github.stivengjekaj.zettastream.settings.SubtitleFont
import io.github.stivengjekaj.zettastream.settings.SubtitleSize
import io.github.stivengjekaj.zettastream.settings.ViewerSettings
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.Screen
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.focusRing
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import io.github.stivengjekaj.zettastream.update.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun SettingsRow(title: String, detail: String, tv: Boolean, onClick: () -> Unit) {
    val shape = Corner
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
private fun Section(title: String, tv: Boolean) {
    Text(
        title.uppercase(), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = Sizes.gutter(tv), end = Sizes.gutter(tv), top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun SettingsScreen(app: AppState) {
    val c = app.container
    val tv = app.isTv
    val scope = rememberCoroutineScope()
    val sources by c.sources.sources.collectAsState()
    val addons by c.addons.addons.collectAsState()
    val viewer by c.settings.settings.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    fun set(change: (ViewerSettings) -> ViewerSettings) { scope.launch { c.settings.update(change) } }
    fun onOff(on: Boolean) = if (on) "On" else "Off"
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
        item { SettingsRow("Home rows", "${viewer.hiddenRows.size} rows off. Turn each row of the home screen off or on.", tv) { app.open(Screen.HomeRows) } }
        item { Section("Viewing", tv) }
        item { SettingsRow("Subtitles", "${onOff(viewer.subtitlesOn)}. The Subtitles button changes them while you watch.", tv) { set { it.copy(subtitlesOn = !it.subtitlesOn) } } }
        item {
            SettingsRow("Subtitle language", SettingsStore.languageName(viewer.subtitleLanguage.ifEmpty { "en" }), tv) {
                set { it.copy(subtitleLanguage = SettingsStore.next(SettingsStore.Languages.drop(1).map { l -> l.first }, it.subtitleLanguage)) }
            }
        }
        item { SettingsRow("Subtitle size", viewer.subtitleSize.label, tv) { set { it.copy(subtitleSize = SettingsStore.next(SubtitleSize.entries, it.subtitleSize)) } } }
        item { SettingsRow("Subtitle text color", viewer.subtitleColor.label, tv) { set { it.copy(subtitleColor = SettingsStore.next(SubtitleColor.entries, it.subtitleColor)) } } }
        item { SettingsRow("Subtitle background", "${viewer.subtitleBackground.label}. A box behind the text.", tv) { set { it.copy(subtitleBackground = SettingsStore.next(SubtitleBackground.entries, it.subtitleBackground)) } } }
        item { SettingsRow("Subtitle font", viewer.subtitleFont.label, tv) { set { it.copy(subtitleFont = SettingsStore.next(SubtitleFont.entries, it.subtitleFont)) } } }
        item { SettingsRow("Subtitle edge", "${viewer.subtitleEdge.label}. An edge keeps text readable with no background.", tv) { set { it.copy(subtitleEdge = SettingsStore.next(SubtitleEdge.entries, it.subtitleEdge)) } } }
        item {
            SettingsRow("Audio language", SettingsStore.languageName(viewer.audioLanguage), tv) {
                set { it.copy(audioLanguage = SettingsStore.next(SettingsStore.Languages.map { l -> l.first }, it.audioLanguage)) }
            }
        }
        item { SettingsRow("Play the next episode", "${onOff(viewer.autoplayNext)}. A countdown at the end of an episode starts the next one.", tv) { set { it.copy(autoplayNext = !it.autoplayNext) } } }
        item { SettingsRow("Skip intros automatically", "${onOff(viewer.autoSkipIntro)}. The app jumps over the opening and the recap of an anime episode when it knows their times.", tv) { set { it.copy(autoSkipIntro = !it.autoSkipIntro) } } }
        item { SettingsRow("Press the channel buttons twice", "${onOff(viewer.confirmChannel)}. When off, one press changes the episode or the channel.", tv) { set { it.copy(confirmChannel = !it.confirmChannel) } } }
        item { Section("Torrents", tv) }
        item { SettingsRow("Show torrent streams", "${onOff(viewer.showTorrents)}. Torrent streams connect you to other people, who can see your IP address.", tv) { set { it.copy(showTorrents = !it.showTorrents) } } }
        item { SettingsRow("Direct links first", "${onOff(viewer.directFirst)}. Direct links show above torrents in the list of streams.", tv) { set { it.copy(directFirst = !it.directFirst) } } }
        item { SettingsRow("Share while streaming", "${onOff(viewer.torrentUpload)}. When off, the app uploads almost nothing.", tv) { set { it.copy(torrentUpload = !it.torrentUpload) } } }
        item { SettingsRow("VPN notice at start", onOff(viewer.vpnNotice), tv) { set { it.copy(vpnNotice = !it.vpnNotice) } } }
        item { Section("App", tv) }
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
                        UpdateChecker.isNewer(latest, BuildConfig.VERSION_NAME) -> "Version $latest is available. Open Downloader and enter the code ${UpdateChecker.DOWNLOADER_CODE}"
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
                TextButton(shape = Corner, onClick = { scope.launch { c.library.clearHistory() }; confirmClear = false }, modifier = Modifier.focusRing()) { Text("Clear") }
            },
            dismissButton = { TextButton(shape = Corner, onClick = { confirmClear = false }, modifier = Modifier.focusRing()) { Text("Cancel") } },
        )
    }
}
