package io.github.stivengjekaj.zettastream.torrent

import io.github.stivengjekaj.zettastream.settings.ViewerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import org.libtorrent4j.SessionHandle
import java.io.File
import java.io.IOException
import java.net.URLEncoder

/** One file of a torrent that the local server streams. */
class TorrentFile(
    val handle: TorrentHandle,
    val info: TorrentInfo,
    val index: Int,
    val file: File,
) {
    val name: String = info.files().fileName(index)
    val size: Long = info.files().fileSize(index)
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
    private val open = mutableMapOf<String, TorrentFile>()

    init {
        // A torrent from an earlier run is not needed. Remove it.
        scope.launch(Dispatchers.IO) { dir.deleteRecursively(); dir.mkdirs() }
    }

    private fun session(): SessionManager = session ?: SessionManager(false).also {
        it.start(SessionParams(settingsPack()))
        session = it
    }

    private fun settingsPack(): SettingsPack = SettingsPack()
        .connectionsLimit(200)
        .activeDownloads(4)
        // libtorrent reads 0 as no limit, so 1 KB/s is the lowest upload limit.
        .uploadRateLimit(if (settings.value.torrentUpload) 0 else MIN_UPLOAD)
        .seedingOutgoingConnections(false)

    /**
     * Gets the metadata of the torrent, starts the download of one file, and
     * gives the local URL of that file. This can take up to a minute.
     */
    suspend fun open(infoHash: String, sources: List<String>, fileIdx: Int?, name: String?): String = withContext(Dispatchers.IO) {
        val hash = infoHash.lowercase()
        open[hash]?.let { return@withContext server.url(hash, it) }
        val session = session()
        session.applySettings(settingsPack())
        val bytes = session.fetchMagnet(TorrentMath.magnet(hash, sources, name), METADATA_TIMEOUT, dir)
            ?: throw IOException("No peer sent the torrent information in ${METADATA_TIMEOUT} seconds")
        val info = TorrentInfo(bytes)
        val files = info.files()
        val names = (0 until files.numFiles()).map { files.fileName(it) }
        val sizes = (0 until files.numFiles()).map { files.fileSize(it) }
        val index = TorrentMath.chooseFile(names, sizes, fileIdx)
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
        if (open.isEmpty()) scope.launch(Dispatchers.IO) { runCatching { session?.pause() } }
    }

    companion object {
        const val MIN_UPLOAD = 1024
        const val METADATA_TIMEOUT = 60

        fun pathName(name: String): String = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
    }
}
