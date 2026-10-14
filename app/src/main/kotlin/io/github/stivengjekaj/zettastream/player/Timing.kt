package io.github.stivengjekaj.zettastream.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ForwardingRenderer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import java.util.Locale

/**
 * The timing that the viewer sets in the player. A positive subtitle delay shows the
 * subtitles later. A positive audio delay plays the sound later than the picture.
 */
class Timing {
    @Volatile var subtitleDelayMs = 0L
    @Volatile var audioDelayMs = 0L

    companion object {
        const val SUBTITLE_STEP = 250L
        const val SUBTITLE_LIMIT = 30_000L
        const val AUDIO_STEP = 50L
        const val AUDIO_LIMIT = 2_000L

        /** The next value after one step, kept inside the limit. */
        fun step(current: Long, direction: Int, step: Long, limit: Long): Long =
            (current + direction * step).coerceIn(-limit, limit)

        /** A delay for the options panel, such as "+1.25 s" or "None". */
        fun label(ms: Long): String = when {
            ms == 0L -> "None"
            else -> String.format(Locale.ROOT, "%+.2f s", ms / 1000.0).replace(".00 s", " s").replace(Regex("""(\.\d)0 s$"""), "$1 s")
        }
    }
}

/** Gives a renderer a playback position that is [shiftUs] earlier, so that it shows its output later. */
@OptIn(UnstableApi::class)
private class ShiftedRenderer(delegate: Renderer, private val shiftUs: () -> Long) : ForwardingRenderer(delegate) {
    override fun render(positionUs: Long, elapsedRealtimeUs: Long) = super.render(positionUs - shiftUs(), elapsedRealtimeUs)
}

/**
 * Builds the usual renderers, and shifts the subtitles and the picture by [timing].
 * The sound is the clock of the player, so an audio delay moves the picture instead.
 */
@OptIn(UnstableApi::class)
class TimingRenderersFactory(context: Context, private val timing: Timing) : DefaultRenderersFactory(context) {
    override fun buildTextRenderers(
        context: Context, output: TextOutput, outputLooper: Looper, extensionRendererMode: Int, out: ArrayList<Renderer>,
    ) {
        val built = ArrayList<Renderer>()
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, built)
        built.mapTo(out) { ShiftedRenderer(it) { timing.subtitleDelayMs * 1000 } }
    }

    override fun buildVideoRenderers(
        context: Context, extensionRendererMode: Int, mediaCodecSelector: MediaCodecSelector, enableDecoderFallback: Boolean,
        eventHandler: Handler, eventListener: VideoRendererEventListener, allowedVideoJoiningTimeMs: Long, out: ArrayList<Renderer>,
    ) {
        val built = ArrayList<Renderer>()
        super.buildVideoRenderers(context, extensionRendererMode, mediaCodecSelector, enableDecoderFallback, eventHandler, eventListener, allowedVideoJoiningTimeMs, built)
        // Sound later is the same as the picture earlier.
        built.mapTo(out) { ShiftedRenderer(it) { -timing.audioDelayMs * 1000 } }
    }
}
