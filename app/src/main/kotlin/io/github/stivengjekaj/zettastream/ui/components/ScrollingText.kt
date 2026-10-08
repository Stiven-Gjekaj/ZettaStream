package io.github.stivengjekaj.zettastream.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/**
 * Text that ends in "..." when it is too long. When [active] is true, the
 * text shows on one line and scrolls, so that the viewer can read all of it.
 * The space stays the same in both states, so that nothing around it moves.
 */
@Composable
fun ScrollingText(
    text: String,
    active: Boolean,
    color: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    maxLines: Int,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    val height = with(LocalDensity.current) { (lineHeight * maxLines).toDp() }
    Box(modifier.heightIn(min = height), contentAlignment = Alignment.TopStart) {
        if (active) {
            Text(
                text, color = color, fontSize = fontSize, lineHeight = lineHeight, fontWeight = fontWeight, maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, initialDelayMillis = 1200, repeatDelayMillis = 1500),
            )
        } else {
            Text(
                text, color = color, fontSize = fontSize, lineHeight = lineHeight, fontWeight = fontWeight,
                maxLines = maxLines, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
