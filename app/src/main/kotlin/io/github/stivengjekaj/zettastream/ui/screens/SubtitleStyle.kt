package io.github.stivengjekaj.zettastream.ui.screens

import android.graphics.Color
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import io.github.stivengjekaj.zettastream.settings.SubtitleEdge
import io.github.stivengjekaj.zettastream.settings.SubtitleFont
import io.github.stivengjekaj.zettastream.settings.ViewerSettings

/** The parts of a subtitle style as plain values, so that a test can check them. */
data class SubtitleLook(val foreground: Int, val background: Int, val edgeType: Int, val edgeColor: Int)

object SubtitleStyle {
    @OptIn(UnstableApi::class)
    fun look(settings: ViewerSettings): SubtitleLook = SubtitleLook(
        foreground = settings.subtitleColor.argb,
        background = Color.argb(settings.subtitleBackground.alpha, 0, 0, 0),
        edgeType = when (settings.subtitleEdge) {
            SubtitleEdge.None -> CaptionStyleCompat.EDGE_TYPE_NONE
            SubtitleEdge.Outline -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
            SubtitleEdge.Shadow -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            SubtitleEdge.Raised -> CaptionStyleCompat.EDGE_TYPE_RAISED
        },
        edgeColor = Color.BLACK,
    )

    private fun typeface(font: SubtitleFont): Typeface? = when (font) {
        SubtitleFont.Default -> null
        SubtitleFont.Sans -> Typeface.SANS_SERIF
        SubtitleFont.Serif -> Typeface.SERIF
        SubtitleFont.Mono -> Typeface.MONOSPACE
        SubtitleFont.Bold -> Typeface.DEFAULT_BOLD
    }

    /** Applies the style of the viewer. The style of the viewer wins over a style in the subtitle file. */
    @OptIn(UnstableApi::class)
    fun apply(view: SubtitleView?, settings: ViewerSettings) {
        view ?: return
        val l = look(settings)
        view.setApplyEmbeddedStyles(false)
        view.setStyle(CaptionStyleCompat(l.foreground, l.background, Color.TRANSPARENT, l.edgeType, l.edgeColor, typeface(settings.subtitleFont)))
        view.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * settings.subtitleSize.scale)
    }
}
