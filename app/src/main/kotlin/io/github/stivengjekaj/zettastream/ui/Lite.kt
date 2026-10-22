package io.github.stivengjekaj.zettastream.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.stivengjekaj.zettastream.settings.LiteMode

/**
 * Lite mode for a weak device. It removes the work that a slow TV feels on each key press:
 * the scale animations, the moving banner, large images, and the ambient blur.
 * The screens read [on], so a change in Settings takes effect at once.
 */
object Lite {
    var on by mutableStateOf(false)

    /** Lite mode is on when the viewer says so, or for Auto on weak hardware. */
    fun resolve(mode: LiteMode, weak: Boolean): Boolean = when (mode) {
        LiteMode.Auto -> weak
        LiteMode.On -> true
        LiteMode.Off -> false
    }
}
