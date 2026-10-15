package io.github.stivengjekaj.zettastream.player

/**
 * The ambient fill of the side bars. The player copies the frame at a tiny size a few times a
 * second, blurs it here, and stretches it behind the video. The video itself is not changed.
 */
object Ambient {
    /** The size of the copy of the frame. A small copy costs almost nothing. */
    const val WIDTH = 32
    const val HEIGHT = 18

    /** The time between two copies while the video plays. */
    const val INTERVAL_MS = 500L

    /** A change of colour smaller than this, on average for each part of a pixel, is not drawn. */
    const val MIN_CHANGE = 6

    /**
     * True when the video does not fill the screen, so the side bars have space. A video
     * within 3 percent of the screen shape needs no fill.
     */
    fun needed(videoWidth: Int, videoHeight: Int, pixelRatio: Float, screenWidth: Int, screenHeight: Int): Boolean {
        if (videoWidth <= 0 || videoHeight <= 0 || screenWidth <= 0 || screenHeight <= 0) return false
        val video = videoWidth * pixelRatio / videoHeight
        val screen = screenWidth.toFloat() / screenHeight
        return kotlin.math.abs(video - screen) / screen > 0.03f
    }

    /** A box blur of [radius] on ARGB [pixels], two times in each direction, which looks soft. */
    fun blur(pixels: IntArray, width: Int, height: Int, radius: Int): IntArray {
        var out = pixels
        repeat(2) {
            out = pass(out, width, height, radius, horizontal = true)
            out = pass(out, width, height, radius, horizontal = false)
        }
        return out
    }

    private fun pass(src: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean): IntArray {
        val dst = IntArray(src.size)
        val lines = if (horizontal) h else w
        val length = if (horizontal) w else h
        for (line in 0 until lines) {
            for (i in 0 until length) {
                var a = 0; var red = 0; var g = 0; var b = 0; var n = 0
                for (k in (i - r).coerceAtLeast(0)..(i + r).coerceAtMost(length - 1)) {
                    val p = src[if (horizontal) line * w + k else k * w + line]
                    a += p ushr 24; red += p shr 16 and 0xFF; g += p shr 8 and 0xFF; b += p and 0xFF; n++
                }
                dst[if (horizontal) line * w + i else i * w + line] = (a / n shl 24) or (red / n shl 16) or (g / n shl 8) or (b / n)
            }
        }
        return dst
    }

    /**
     * The average change of each colour part between two copies. A small change is not drawn
     * again, because each draw of the fill covers the full screen.
     */
    fun change(a: IntArray, b: IntArray): Int {
        if (a.size != b.size || a.isEmpty()) return Int.MAX_VALUE
        var sum = 0L
        for (i in a.indices) {
            val x = a[i]; val y = b[i]
            sum += kotlin.math.abs((x shr 16 and 0xFF) - (y shr 16 and 0xFF)) +
                kotlin.math.abs((x shr 8 and 0xFF) - (y shr 8 and 0xFF)) +
                kotlin.math.abs((x and 0xFF) - (y and 0xFF))
        }
        return (sum / (a.size * 3)).toInt()
    }
}
