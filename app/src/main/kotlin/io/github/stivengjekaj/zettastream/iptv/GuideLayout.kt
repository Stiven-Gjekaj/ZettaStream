package io.github.stivengjekaj.zettastream.iptv

/** One programme in the guide grid. [offset] and [width] are parts of the time window, from 0 to 1. */
data class GuideBlock(val programme: Programme, val offset: Float, val width: Float)

/** Places the programmes of a channel on the time line of the guide grid. */
object GuideLayout {
    const val HALF_HOUR = 30 * 60_000L

    /** The grid shows three hours from the last full or half hour, so the programme on now is in it. */
    const val SPAN = 6 * HALF_HOUR

    fun windowStart(now: Long): Long = now - now % HALF_HOUR

    /** The programmes that are in the window, cut at its edges. */
    fun blocks(programmes: List<Programme>, from: Long, span: Long = SPAN): List<GuideBlock> {
        val to = from + span
        return programmes.filter { it.stop > from && it.start < to && it.stop > it.start }.sortedBy { it.start }.map {
            val start = maxOf(it.start, from)
            val stop = minOf(it.stop, to)
            GuideBlock(it, (start - from).toFloat() / span, (stop - start).toFloat() / span)
        }
    }
}
