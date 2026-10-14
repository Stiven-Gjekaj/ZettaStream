package io.github.stivengjekaj.zettastream.remote

/**
 * Decides if Left can leave a row and go into the icon bar. Only a new press
 * can: a held Left, or a Left that comes less than [WINDOW] milliseconds after
 * the Left before it, stops at the first item of the row. A fast run of Left
 * presses along a row then does not open the bar by accident.
 */
class EdgeGuard(private val clock: () -> Long = System::currentTimeMillis) {
    private var lastLeft = Long.MIN_VALUE / 2

    /** True when the last Left press can go into the icon bar. */
    var allowed = false
        private set

    fun onLeft(repeat: Boolean) {
        val now = clock()
        allowed = !repeat && now - lastLeft >= WINDOW
        lastLeft = now
    }

    companion object {
        const val WINDOW = 400L
    }
}
