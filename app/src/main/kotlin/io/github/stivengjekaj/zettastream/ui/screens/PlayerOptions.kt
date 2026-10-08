package io.github.stivengjekaj.zettastream.ui.screens

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import io.github.stivengjekaj.zettastream.ui.theme.Accent
import io.github.stivengjekaj.zettastream.ui.theme.Corner
import io.github.stivengjekaj.zettastream.ui.theme.OnAccent
import io.github.stivengjekaj.zettastream.ui.theme.Surface
import io.github.stivengjekaj.zettastream.ui.theme.TextPrimary
import io.github.stivengjekaj.zettastream.ui.theme.TextSecondary
import java.util.Locale

/** One track that the viewer can choose, or "Off" for subtitles when [group] is null. */
data class TrackChoice(val label: String, val group: Tracks.Group?, val index: Int)

/** One row of the options panel. Left and Right change the value. OK runs [onSelect] when it is set. */
data class OptionRow(
    val label: String,
    val value: String,
    val onPrevious: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onSelect: (() -> Unit)? = null,
)

val PlaybackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(UnstableApi::class)
object TrackChoices {
    fun choices(player: Player, type: Int): List<TrackChoice> {
        val tracks = player.currentTracks.groups.filter { it.type == type }.flatMap { g ->
            (0 until g.length).filter { g.isTrackSupported(it) }.mapIndexed { n, i ->
                val f = g.getTrackFormat(i)
                val name = f.label ?: f.language?.let { Locale.forLanguageTag(it).displayLanguage }?.ifBlank { null } ?: "Track ${n + 1}"
                TrackChoice(name, g, i)
            }
        }
        return if (type == C.TRACK_TYPE_TEXT) listOf(TrackChoice("Off", null, -1)) + tracks else tracks
    }

    fun current(player: Player, type: Int, choices: List<TrackChoice>): Int {
        if (type == C.TRACK_TYPE_TEXT && C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes) return 0
        val i = choices.indexOfFirst { it.group?.isTrackSelected(it.index) == true }
        return if (i >= 0) i else 0
    }

    fun select(player: Player, type: Int, choice: TrackChoice) {
        val builder = player.trackSelectionParameters.buildUpon()
        player.trackSelectionParameters = if (choice.group == null) {
            builder.setTrackTypeDisabled(type, true).build()
        } else {
            builder.setTrackTypeDisabled(type, false)
                .setOverrideForType(TrackSelectionOverride(choice.group.mediaTrackGroup, choice.index)).build()
        }
    }
}

/** The options panel on the right side of the player. */
@Composable
fun PlayerOptionsPanel(rows: List<OptionRow>, focused: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .width(440.dp)
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Options", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Up and Down: choose. Left and Right: change. Back: close.", color = TextSecondary, fontSize = 12.sp)
        Spacer(Modifier.padding(4.dp))
        // Show at most eight rows, with the focused row in view.
        val first = (focused - 4).coerceIn(0, (rows.size - 8).coerceAtLeast(0))
        rows.forEachIndexed { i, row ->
            if (i < first || i >= first + 8) return@forEachIndexed
            val on = i == focused
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(Corner)
                    .background(if (on) Accent else Surface)
                    .border(1.dp, if (on) Accent else Color(0xFF2E2E2E), Corner)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.label, color = if (on) OnAccent else TextSecondary, fontSize = 15.sp, modifier = Modifier.width(150.dp))
                Text(
                    if (row.onSelect != null) row.value else "‹  ${row.value}  ›",
                    color = if (on) OnAccent else TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
