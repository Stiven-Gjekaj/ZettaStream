package io.github.stivengjekaj.zettastream.library

/** Decides if a saved position fits the source that plays now. */
object Resume {
    /** A position this near the end does not resume, because the video would end at once. */
    const val END_MARGIN = 15_000L

    /** True when [position] is before the end of a video of [duration] milliseconds, with a margin. */
    fun fits(position: Long, duration: Long): Boolean = duration <= 0 || position < duration - END_MARGIN
}
