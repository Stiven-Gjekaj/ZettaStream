package io.github.stivengjekaj.zettastream.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import io.github.stivengjekaj.zettastream.addon.Addon
import io.github.stivengjekaj.zettastream.pairing.PairingServer
import io.github.stivengjekaj.zettastream.source.Source
import io.github.stivengjekaj.zettastream.source.SourceKind
import io.github.stivengjekaj.zettastream.source.SourceList
import io.github.stivengjekaj.zettastream.ui.AppState
import io.github.stivengjekaj.zettastream.ui.components.Sizes
import io.github.stivengjekaj.zettastream.ui.components.ZButton
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Danger
import io.github.stivengjekaj.zettastream.ui.theme.SurfaceHigh
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import kotlinx.coroutines.launch

fun qrBitmap(text: String, size: Int = 512): Bitmap {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}

@Composable
fun SourcesScreen(app: AppState) {
    val c = app.container
    val tv = app.isTv
    val scope = rememberCoroutineScope()
    val sources by c.sources.sources.collectAsState()
    val addons by c.addons.addons.collectAsState()
    val live by c.live.live.collectAsState()
    var pairUrl by remember { mutableStateOf<String?>(null) }
    var received by remember { mutableStateOf<Int?>(null) }
    var restart by remember { mutableIntStateOf(0) }
    var draft by remember(sources) { mutableStateOf(SourceList.format(sources)) }

    if (tv) {
        DisposableEffect(restart) {
            val server = PairingServer(
                scope = c.scope,
                currentText = { SourceList.format(c.sources.sources.value) },
                onLines = { text -> c.sources.replace(text).size },
                onDone = { count -> c.scope.launch { received = count; restart++ } },
            )
            pairUrl = server.start()
            onDispose { server.stop() }
        }
    }

    fun status(source: Source): Pair<String, Boolean> = when (source.kind) {
        SourceKind.Addon -> {
            val url = Addon.normalizeManifestUrl(source.url)
            addons.addons.firstOrNull { it.manifestUrl == url }?.let {
                ("Addon: ${it.name}" + (if (it.manifest.configurationRequired) " (needs configuration)" else "")) to true
            }
                ?: addons.failures[source.url]?.let { "Addon does not answer: $it" to false }
                ?: ("Addon: loading" to true)
        }
        SourceKind.Playlist -> live.failures[source.url]?.let { "Playlist does not load: $it" to false } ?: ("Playlist" to true)
        SourceKind.Guide -> live.failures[source.url]?.let { "Guide does not load: $it" to false } ?: ("TV guide" to true)
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = if (tv) 32.dp else 16.dp)) {
        item {
            Text("Sources", color = TextPrimary, fontSize = if (tv) 30.sp else 24.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv)))
        }
        if (tv) {
            item {
                Row(Modifier.padding(Sizes.gutter(tv)), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    val url = pairUrl
                    Box(Modifier.size(260.dp).clip(Corner).background(Color.White), contentAlignment = Alignment.Center) {
                        if (url != null) {
                            val bitmap = remember(url) { qrBitmap(url).asImageBitmap() }
                            Image(bitmap, "QR code for the pairing page", filterQuality = FilterQuality.None, modifier = Modifier.size(244.dp))
                        } else {
                            Text("No network", color = Color.Black)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add sources from your phone", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text("1. Connect the phone to the same Wi-Fi as the TV.", color = TextSecondary, fontSize = 16.sp)
                        Text("2. Scan the QR code with the camera of the phone.", color = TextSecondary, fontSize = 16.sp)
                        Text("3. Paste your list. Put one URL on each line.", color = TextSecondary, fontSize = 16.sp)
                        Text("4. Press Send. The new list replaces the old list.", color = TextSecondary, fontSize = 16.sp)
                        if (url != null) Text(url, color = Accent, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                        else Text("The TV has no local network address. Connect it to Wi-Fi or Ethernet.", color = Danger, fontSize = 15.sp)
                        received?.let { Text("Received $it sources. The QR code is new now.", color = Accent, fontSize = 16.sp) }
                    }
                }
            }
        } else {
            item {
                Column(Modifier.padding(Sizes.gutter(tv))) {
                    Text("Paste one URL on each line: a Stremio addon manifest.json, an M3U playlist, or an XMLTV guide.",
                        color = TextSecondary, fontSize = 14.sp)
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextPrimary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp).padding(vertical = 10.dp),
                    )
                    ZButton(onClick = { scope.launch { received = c.sources.replace(draft).size } }, tv = tv, primary = true) {
                        Text("Save")
                    }
                    received?.let { Text("Saved $it sources.", color = Accent, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }
        item {
            Text("Current list (${sources.size})", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Sizes.gutter(tv), vertical = 8.dp))
        }
        items(sources, key = { it.url }) { source ->
            val (text, ok) = status(source)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = Sizes.gutter(tv), vertical = 4.dp)
                    .clip(Corner).background(SurfaceHigh).padding(12.dp),
            ) {
                Text(text, color = if (ok) TextPrimary else Danger, fontSize = 15.sp)
                // Hide the path: a configured addon URL can hold a key.
                Text(source.url.substringBefore("://") + "://" + source.url.substringAfter("://").substringBefore('/') + "/...",
                    color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
