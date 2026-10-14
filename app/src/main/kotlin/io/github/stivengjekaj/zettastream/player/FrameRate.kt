package io.github.stivengjekaj.zettastream.player

import kotlin.math.abs
import kotlin.math.roundToInt

/** One display mode of the TV. */
data class DisplayMode(val id: Int, val width: Int, val height: Int, val refreshRate: Float)

/**
 * Chooses the display mode for a video, so that each frame shows for the same
 * time. A 24 fps film on a 60 Hz screen shows frames for 2, then 3 refreshes,
 * and a slow pan judders.
 */
object FrameRate {
    /**
     * The mode with the size of [current] and the lowest refresh rate that is a
     * whole multiple of [fps]. Null when no mode fits, or when [current] fits already.
     */
    fun choose(modes: List<DisplayMode>, current: DisplayMode, fps: Float): DisplayMode? {
        if (fps <= 0f) return null
        val fits = modes
            .filter { it.width == current.width && it.height == current.height && isMultiple(it.refreshRate, fps) }
            .minByOrNull { it.refreshRate } ?: return null
        return if (isMultiple(current.refreshRate, fps) && current.refreshRate <= fits.refreshRate) null else fits
    }

    /** True when [rate] is 1, 2, 3, ... times [fps], with 0.05 percent of room. 23.976 and 24 are 0.1 percent apart, so they do not match. */
    fun isMultiple(rate: Float, fps: Float): Boolean {
        val times = (rate / fps).roundToInt()
        return times >= 1 && abs(rate - times * fps) <= rate * 0.0005f
    }

    /**
     * The frame rate from the times of the first frames, for a video that does
     * not tell its rate. A missed frame does not change it. Null with too few frames.
     */
    fun fromTimes(timesUs: List<Long>): Float? {
        val sorted = timesUs.distinct().sorted()
        val gaps = sorted.zipWithNext { a, b -> b - a }.sorted()
        if (gaps.size < 8) return null
        // One gap is rounded to the microsecond. The whole span is exact, so count the frames in it with the middle gap.
        val span = sorted.last() - sorted.first()
        val frames = (span.toDouble() / gaps[gaps.size / 2]).roundToInt()
        return (frames * 1_000_000.0 / span).toFloat()
    }

    /** The common rate that is nearest to [measured], such as 23.976 for 23.98. Measured times are rounded to the microsecond. */
    fun nearestCommon(measured: Float): Float =
        listOf(23.976f, 24f, 25f, 29.97f, 30f, 47.952f, 48f, 50f, 59.94f, 60f).minBy { abs(it - measured) }
}

