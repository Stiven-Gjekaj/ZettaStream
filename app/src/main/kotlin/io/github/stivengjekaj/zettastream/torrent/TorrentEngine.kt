package io.github.stivengjekaj.zettastream.torrent

import io.github.stivengjekaj.zettastream.settings.ViewerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.swig.settings_pack
import org.libtorrent4j.SessionHandle
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** One file of a torrent that the local server streams. */
class TorrentFile(
    val handle: TorrentHandle,
    val info: TorrentInfo,
    val index: Int,
    val file: File,
) {
    val name: String = info.files().fileName(index)
    val size: Long = info.files().fileSize(index)
    val firstPiece: Int = info.mapFile(index, 0, 1).piece()
    val lastPiece: Int = info.mapFile(index, maxOf(0, size - 1), 1).piece()
    private val ahead = TorrentMath.piecesFor(WINDOW_BYTES, info.pieceLength())
    private val edge = TorrentMath.piecesFor(EDGE_BYTES, info.pieceLength())
    @Volatile private var focusPiece = -1
    private val deadlines = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    /**
     * Asks libtorrent for the next pieces first. Each piece gets its deadline
     * one time only: a new deadline restarts the request for that piece, so
     * a deadline that is set again and again delays the piece.
     */
    fun urgent(fromPiece: Int, count: Int, step: Int) {
        for (k in 0 until count) {
            val p = fromPiece + k
            if (p > lastPiece) break
            if (p in deadlines || handle.havePiece(p)) continue
            deadlines += p
            handle.setPieceDeadline(p, (k + 1) * step)
        }
    }

    /**
     * Before the player reads, downloads only the head and the tail of the
     * file. The player then asks for the position where it starts, which can
     * be far from the head when it resumes.
     */
    fun prime() {
        val wanted = TorrentMath.wanted(firstPiece, lastPiece, firstPiece, 0, edge)
        runCatching { handle.prioritizePieces(Array(info.numPieces()) { if (it in wanted) Priority.TOP_PRIORITY else Priority.IGNORE }) }
    }

    /**
     * Downloads only the head, the tail, and a window ahead of [readPiece].
     * It changes the priorities again only when the read position moved a
     * quarter of the window, because each change costs work in libtorrent.
     */
    fun focus(readPiece: Int) {
        if (focusPiece >= 0 && readPiece >= focusPiece && readPiece - focusPiece < ahead / 4) return
        focusPiece = readPiece
        val wanted = TorrentMath.wanted(firstPiece, lastPiece, readPiece, ahead, edge)
        val priorities = Array(info.numPieces()) { if (it in wanted) Priority.TOP_PRIORITY else Priority.IGNORE }
        runCatching { handle.prioritizePieces(priorities) }
    }

    companion object {
        /** About two to five minutes of video at the bitrates of most releases. */
        const val WINDOW_BYTES = 96L * 1024 * 1024
        /** The head and the tail hold the index that a player reads before it plays. */
        const val EDGE_BYTES = 8L * 1024 * 1024
    }
}

data class TorrentState(val peers: Int, val downloadRate: Int, val progress: Float)

/**
 * Downloads a torrent in order and serves the chosen file on a local HTTP
 * port, so that the player can play it before the download is complete.
 */
class TorrentEngine(
    private val dir: File,
    private val settings: StateFlow<ViewerSettings>,
    private val scope: CoroutineScope,
) {
    private var session: SessionManager? = null
    private val server = TorrentServer(scope)
    // The player reads this map on the main thread, and open() writes it on an IO thread.
    private val open = ConcurrentHashMap<String, TorrentFile>()
    private val opening = AtomicInteger()

    init {
        // A torrent from an earlier run is not needed. Remove it.
        scope.launch(Dispatchers.IO) { dir.deleteRecursively(); dir.mkdirs() }
    }

    @Synchronized
    private fun session(): SessionManager = session ?: SessionManager(false).also {
        it.start(SessionParams(settingsPack()))
        session = it
    }

    private fun settingsPack(): SettingsPack = SettingsPack()
        .connectionsLimit(200)
        .activeDownloads(4)
        // libtorrent reads 0 as no limit. A few KB/s keeps peers willing to send.
        .uploadRateLimit(if (settings.value.torrentUpload) 0 else MIN_UPLOAD)
        .seedingOutgoingConnections(false)
        // Without this, the requests for pieces count against the upload limit,
        // and a low limit stops the download too.
        .setBoolean(settings_pack.bool_types.rate_limit_ip_overhead.swigValue(), false)

    /**
     * Gets the metadata of the torrent, starts the download of one file, and
     * gives the local URL of that file. This can take up to a minute.
     */
    suspend fun open(
        infoHash: String,
        sources: List<String>,
        fileIdx: Int?,
        name: String?,
        filename: String? = null,
        season: Int? = null,
        episode: Int? = null,
    ): String = withContext(Dispatchers.IO) {
        opening.incrementAndGet()
        try { start(infoHash, sources, fileIdx, name, filename, season, episode) } finally { opening.decrementAndGet() }
    }

    private suspend fun start(
        infoHash: String,
        sources: List<String>,
        fileIdx: Int?,
        name: String?,
        filename: String?,
        season: Int?,
        episode: Int?,
    ): String = coroutineScope {
        val hash = infoHash.lowercase()
        open[hash]?.let { return@coroutineScope server.url(hash, it) }
        val session = session()
        // close() pauses the session when the last torrent closes. A paused session starts no new torrent.
        session.resume()
        session.applySettings(settingsPack())
        val bytes = session.fetchMagnet(TorrentMath.magnet(hash, sources, name), METADATA_TIMEOUT, dir)
            ?: throw IOException("No peer sent the torrent information in ${METADATA_TIMEOUT} seconds")
        val info = TorrentInfo(bytes)
        val files = info.files()
        val names = (0 until files.numFiles()).map { files.fileName(it) }
        val sizes = (0 until files.numFiles()).map { files.fileSize(it) }
        val index = TorrentMath.chooseFile(names, sizes, fileIdx, filename, season, episode)
        if (index < 0) throw IOException("The torrent has no file")
        val priorities = Array(files.numFiles()) { if (it == index) Priority.TOP_PRIORITY else Priority.IGNORE }
        session.download(info, dir, null, priorities, null, TorrentFlags.SEQUENTIAL_DOWNLOAD)
        var handle: TorrentHandle? = null
        repeat(50) {
            handle = session.find(Sha1Hash.parseHex(hash)) ?: session.find(info.infoHash())
            if (handle != null) return@repeat
            Thread.sleep(100)
        }
        val h = handle ?: throw IOException("The torrent did not start")
        val torrent = TorrentFile(h, info, index, File(dir, files.filePath(index)))
        torrent.prime()
        open[hash] = torrent
        server.url(hash, torrent)
    }

    fun state(infoHash: String): TorrentState? {
        val t = open[infoHash.lowercase()] ?: return null
        val s = runCatching { t.handle.status() }.getOrNull() ?: return null
        return TorrentState(s.numPeers(), s.downloadRate(), s.progress())
    }

    /** Stops the torrent and deletes its files. The app does not seed after playback. */
    fun close(infoHash: String) {
        val hash = infoHash.lowercase()
        val t = open.remove(hash) ?: return
        server.remove(hash)
        runCatching { session?.remove(t.handle, SessionHandle.DELETE_FILES) }
        scope.launch(Dispatchers.IO) { if (open.isEmpty() && opening.get() == 0) runCatching { session?.pause() } }
    }

    companion object {
        const val MIN_UPLOAD = 4 * 1024
        const val METADATA_TIMEOUT = 60

        fun pathName(name: String): String = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
    }
}
