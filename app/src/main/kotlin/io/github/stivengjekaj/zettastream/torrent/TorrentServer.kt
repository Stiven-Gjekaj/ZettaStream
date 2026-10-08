package io.github.stivengjekaj.zettastream.torrent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Serves the files of open torrents on 127.0.0.1. Only this device can
 * connect. For each byte range, it asks libtorrent for the pieces first and
 * waits until they arrive.
 */
class TorrentServer(private val scope: CoroutineScope) {
    private val files = ConcurrentHashMap<String, TorrentFile>()
    private var socket: ServerSocket? = null

    fun url(hash: String, file: TorrentFile): String {
        files[hash] = file
        val port = start()
        return "http://127.0.0.1:$port/$hash/${TorrentEngine.pathName(file.name)}"
    }

    fun remove(hash: String) {
        files.remove(hash)
    }

    @Synchronized
    private fun start(): Int {
        socket?.let { return it.localPort }
        // The URL says 127.0.0.1, so bind that address. The loopback address can be ::1 on Android.
        val server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        socket = server
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                launch { runCatching { handle(client) } }
            }
        }
        return server.localPort
    }

    private fun handle(client: Socket) = client.use { s ->
        val input = BufferedInputStream(s.getInputStream())
        val head = StringBuilder()
        while (!head.endsWith("\r\n\r\n")) {
            val b = input.read()
            if (b < 0 || head.length > 8192) return
            head.append(b.toChar())
        }
        val lines = head.toString().split("\r\n")
        val parts = lines.first().split(' ')
        val method = parts.getOrNull(0) ?: return
        val hash = parts.getOrNull(1)?.trimStart('/')?.substringBefore('/') ?: return
        val range = lines.firstOrNull { it.startsWith("Range:", ignoreCase = true) }?.substringAfter(':')?.trim()
        val out = s.getOutputStream()
        val torrent = files[hash] ?: return status(out, "404 Not Found")
        val bytes = TorrentMath.range(range, torrent.size)
            ?: return status(out, "416 Range Not Satisfiable", "Content-Range: bytes */${torrent.size}\r\n")
        val partial = range != null
        out.write(
            buildString {
                append(if (partial) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                append("Content-Type: ${TorrentMath.mimeType(torrent.name)}\r\n")
                append("Accept-Ranges: bytes\r\n")
                append("Content-Length: ${bytes.last - bytes.first + 1}\r\n")
                if (partial) append("Content-Range: bytes ${bytes.first}-${bytes.last}/${torrent.size}\r\n")
                append("Connection: close\r\n\r\n")
            }.toByteArray(),
        )
        if (method == "HEAD") return
        // The player sends nothing after the request. A read that ends means that the player closed the connection.
        val gone = {
            s.soTimeout = 1
            try { input.read() < 0 } catch (_: SocketTimeoutException) { false }
        }
        send(torrent, hash, bytes, out, gone)
    }

    private fun status(out: OutputStream, status: String, extra: String = "") {
        out.write("HTTP/1.1 $status\r\n${extra}Content-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
    }

    /** Sends [bytes] of the file. Before each piece, it sets deadlines for the pieces ahead and waits for the first. */
    private fun send(t: TorrentFile, hash: String, bytes: LongRange, out: OutputStream, gone: () -> Boolean) {
        val pieceLength = t.info.pieceLength()
        val buffer = ByteArray(CHUNK)
        var position = bytes.first
        var lastPieceSeen = -1
        var raf: RandomAccessFile? = null
        try {
            while (position <= bytes.last) {
                if (files[hash] == null) throw IOException("The torrent is closed")
                val request = t.info.mapFile(t.index, position, 1)
                val piece = request.piece()
                if (piece != lastPieceSeen) {
                    lastPieceSeen = piece
                    t.focus(piece)
                    t.urgent(piece, AHEAD, DEADLINE_STEP)
                }
                while (!t.handle.havePiece(piece)) {
                    if (files[hash] == null) throw IOException("The torrent is closed")
                    // After a seek, this piece can stay ignored. Stop when the player left.
                    if (gone()) throw IOException("The player closed the connection")
                    Thread.sleep(POLL)
                }
                val file = raf ?: RandomAccessFile(t.file, "r").also { raf = it }
                // Send the rest of this piece, or less at the end of the range.
                val leftInPiece = pieceLength - request.start().toLong()
                val count = minOf(leftInPiece, bytes.last - position + 1, CHUNK.toLong()).toInt()
                file.seek(position)
                val read = file.read(buffer, 0, count)
                if (read <= 0) { Thread.sleep(POLL); continue }
                out.write(buffer, 0, read)
                position += read
            }
            out.flush()
        } finally {
            runCatching { raf?.close() }
        }
    }

    private companion object {
        const val CHUNK = 64 * 1024
        const val AHEAD = 12
        const val DEADLINE_STEP = 800
        const val POLL = 50L
    }
}
