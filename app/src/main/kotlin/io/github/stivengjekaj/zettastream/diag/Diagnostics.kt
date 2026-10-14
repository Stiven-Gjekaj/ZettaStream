package io.github.stivengjekaj.zettastream.diag

import android.util.Log
import io.github.stivengjekaj.zettastream.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the last events of the player, the torrent engine, and the addons in
 * memory. The phone page shows them, so that a fault on the TV can be found
 * without a cable. Nothing goes to a server, and nothing goes to a file.
 */
object Diagnostics {
    const val MAX_LINES = 500

    private val lines = ArrayDeque<String>()
    private val time = SimpleDateFormat("HH:mm:ss", Locale.ROOT)

    /**
     * Only a debug build also writes to the Android log. A release build keeps the
     * lines in memory only, because they hold the links of the streams.
     */
    @Volatile var toLogcat = BuildConfig.DEBUG

    @Synchronized
    fun log(tag: String, message: String, error: Throwable? = null) {
        val reason = error?.let { " (${it.javaClass.simpleName}: ${it.message})" }.orEmpty()
        lines.addLast("${time.format(Date())} $tag: $message$reason")
        while (lines.size > MAX_LINES) lines.removeFirst()
        if (toLogcat) runCatching { Log.d("Z$tag", message + reason) }
    }

    @Synchronized
    fun lines(): List<String> = lines.toList()

    @Synchronized
    fun clear() = lines.clear()
}
