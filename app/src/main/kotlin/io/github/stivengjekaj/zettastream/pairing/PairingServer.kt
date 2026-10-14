package io.github.stivengjekaj.zettastream.pairing

import io.github.stivengjekaj.zettastream.diag.Diagnostics
import io.github.stivengjekaj.zettastream.library.Backup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.security.SecureRandom

data class HttpRequest(val method: String, val path: String, val query: Map<String, String>, val body: String)

/** Small HTTP helpers for the pairing page. They are separate so that tests can use them. */
object PairingHttp {
    /** A backup of a long history can be large. Only a request with the token can send one. */
    const val MAX_BODY = 4 * 1024 * 1024

    /** The largest body of any other request. A source list is much smaller. */
    const val SMALL_BODY = 256 * 1024

    /**
     * Reads one request. [maxBody] gives the largest body for the method, the path,
     * and the query, so that a request without the token cannot make the app keep 4 MB.
     */
    fun read(input: InputStream, maxBody: (method: String, path: String, query: Map<String, String>) -> Int = { _, _, _ -> SMALL_BODY }): HttpRequest? {
        val head = StringBuilder()
        while (!head.endsWith("\r\n\r\n")) {
            val b = input.read()
            if (b < 0 || head.length > 16 * 1024) return null
            head.append(b.toChar())
        }
        val lines = head.toString().split("\r\n")
        val parts = lines.first().split(' ')
        if (parts.size < 2) return null
        val length = lines.drop(1).firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
        val target = parts[1]
        val path = target.substringBefore('?')
        val query = form(target.substringAfter('?', ""))
        if (length < 0 || length > minOf(maxBody(parts[0], path, query), MAX_BODY)) return null
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n < 0) break
            read += n
        }
        return HttpRequest(
            method = parts[0],
            path = path,
            query = query,
            body = String(body, 0, read, Charsets.UTF_8),
        )
    }

    /** Reads `a=1&b=2`. A pair with a bad escape, such as a lone `%`, is left out. */
    fun form(text: String): Map<String, String> = text.split('&').filter { it.isNotEmpty() }.mapNotNull {
        runCatching { URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('=', ""), "UTF-8") }.getOrNull()
    }.toMap()

    fun write(output: OutputStream, status: String, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        output.write(
            ("HTTP/1.1 $status\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray(),
        )
        output.write(bytes)
        output.flush()
    }

    fun writeText(output: OutputStream, status: String, text: String) = writeRaw(output, status, "text/plain; charset=utf-8", text, null)

    /** Sends [text] as a file that the browser saves. */
    fun writeFile(output: OutputStream, name: String, text: String) = writeRaw(output, "200 OK", "application/json; charset=utf-8", text, name)

    private fun writeRaw(output: OutputStream, status: String, type: String, text: String, fileName: String?) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        output.write(
            ("HTTP/1.1 $status\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\n" +
                (fileName?.let { "Content-Disposition: attachment; filename=\"$it\"\r\n" } ?: "") + "Connection: close\r\n\r\n").toByteArray(),
        )
        output.write(bytes)
        output.flush()
    }

    fun page(token: String, current: String, message: String? = null): String = """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>ZettaStream sources</title>
        <style>
          body{margin:0;padding:16px;background:#000;color:#f2f2f2;font-family:Karla,ui-sans-serif,system-ui,sans-serif}
          h1{font-size:20px;margin:8px 0 4px;letter-spacing:-0.5px}p{color:#8c8c8c;font-size:13px}
          textarea{width:100%;box-sizing:border-box;height:55vh;background:#0e0e0e;color:#f2f2f2;border:1px solid #2e2e2e;border-radius:8px;padding:10px;font:12px ui-monospace,monospace}
          button{margin-top:12px;width:100%;padding:14px;border:0;border-radius:8px;background:#fff;color:#000;font:600 15px Karla,ui-sans-serif,system-ui,sans-serif}
          .msg{padding:12px;border-radius:8px;background:#181818;color:#f2f2f2}
        </style></head><body>
        <h1>ZettaStream sources</h1>
        ${if (message != null) "<p class=\"msg\">${escape(message)}</p>" else ""}
        <p>Paste one URL on each line: a Stremio addon <code>manifest.json</code>, an M3U playlist, or an XMLTV guide.
        This list replaces the list on the TV.</p>
        <form method="post" action="/?t=${escape(token)}">
        <textarea name="lines" autocapitalize="off" autocorrect="off" spellcheck="false">${escape(current)}</textarea>
        <button type="submit">Send to the TV</button></form>
        <h1>Backup</h1>
        <p>The watchlist, the history, the favourite channels, the settings, and this list, in one file.</p>
        <p><a style="color:#f2f2f2" href="/backup?t=${escape(token)}">Download a backup</a></p>
        <p>Restore a backup. It replaces the data on the TV.</p>
        <input type="file" id="file" accept=".json,application/json" style="color:#f2f2f2">
        <button type="button" onclick="restore()">Restore</button>
        <p id="result" class="msg" style="display:none"></p>
        <script>
        function restore() {
          var f = document.getElementById('file').files[0]; var out = document.getElementById('result');
          if (!f) { out.style.display = 'block'; out.textContent = 'Choose a backup file first.'; return; }
          f.text().then(function (t) { return fetch('/backup?t=${escape(token)}', { method: 'POST', body: t }); })
            .then(function (r) { return r.text(); })
            .then(function (msg) { out.style.display = 'block'; out.textContent = msg; })
            .catch(function () { out.style.display = 'block'; out.textContent = 'The TV did not answer.'; });
        }
        </script>
        <p><a style="color:#f2f2f2" href="/log?t=${escape(token)}">Diagnostics: the last events on the TV</a></p></body></html>
    """.trimIndent()

    /** The last events of the TV, newest last, for a fault report. */
    fun logPage(header: String, lines: List<String>): String = """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <title>ZettaStream diagnostics</title>
        <style>body{margin:0;padding:16px;background:#000;color:#f2f2f2;font-family:Karla,ui-sans-serif,system-ui,sans-serif}
        h1{font-size:20px}p{color:#8c8c8c;font-size:13px}pre{font-family:ui-monospace,Menlo,monospace;white-space:pre-wrap;word-break:break-all;font-size:11px;line-height:1.5}</style></head>
        <body><h1>Diagnostics</h1><p>${escape(header)}</p>
        <p>Copy this text and send it with a fault report. Reload the page for new events.</p>
        <pre>${escape(lines.joinToString("\n").ifEmpty { "No events yet. Play something on the TV, then reload this page." })}</pre></body></html>
    """.trimIndent()

    fun done(count: Int): String = """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>body{margin:0;padding:24px;background:#000;color:#f2f2f2;font-family:Karla,ui-sans-serif,system-ui,sans-serif}</style></head>
        <body><h1>Sent</h1><p>The TV has $count sources now. You can close this page.</p></body></html>
    """.trimIndent()

    fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

/**
 * Serves the pairing page on the local network. The QR code holds a token,
 * and the server accepts one list with that token only.
 */
class PairingServer(
    private val scope: CoroutineScope,
    private val currentText: () -> String,
    private val onLines: suspend (String) -> Int,
    private val onDone: (Int) -> Unit,
    private val backup: Backup? = null,
) {
    private var socket: ServerSocket? = null
    private var job: Job? = null
    @Volatile private var token: String = newToken()
    private val open = java.util.concurrent.atomic.AtomicInteger(0)

    /** Starts the server and gives the URL to show in the QR code. */
    fun start(): String? {
        val address = lanAddress() ?: return null
        val server = runCatching { ServerSocket(PORT) }.getOrElse { ServerSocket(0) }
        socket = server
        token = newToken()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                // Only a few requests at a time. A device on the network cannot fill the memory with many of them.
                if (open.incrementAndGet() > MAX_OPEN) {
                    open.decrementAndGet()
                    runCatching { client.close() }
                    continue
                }
                // A bad request from a device on the network must not stop the app.
                launch { try { runCatching { handle(client) } } finally { open.decrementAndGet() } }
            }
        }
        return "http://$address:${server.localPort}/?t=$token"
    }

    fun stop() {
        job?.cancel()
        runCatching { socket?.close() }
        socket = null
    }

    private fun handle(client: Socket) = client.use { s ->
        s.soTimeout = 15_000
        val request = PairingHttp.read(BufferedInputStream(s.getInputStream())) { method, path, query ->
            val valid = token.isNotEmpty() && query["t"] == token
            if (valid && method == "POST" && path == "/backup") PairingHttp.MAX_BODY else PairingHttp.SMALL_BODY
        } ?: return
        val out = s.getOutputStream()
        val valid = token.isNotEmpty() && request.query["t"] == token
        when {
            request.path !in setOf("/", "/log", "/backup") -> PairingHttp.write(out, "404 Not Found", "Not found")
            !valid -> PairingHttp.write(out, "403 Forbidden", "This code is old. Scan the new QR code on the TV.")
            request.path == "/log" -> PairingHttp.write(out, "200 OK", PairingHttp.logPage(deviceInfo(), Diagnostics.lines()))
            request.path == "/backup" && backup == null -> PairingHttp.write(out, "404 Not Found", "Not found")
            request.path == "/backup" && request.method == "POST" -> {
                val result = runBlocking { backup!!.restore(request.body) }
                PairingHttp.writeText(out, if (result.isSuccess) "200 OK" else "400 Bad Request", result.getOrElse { "The backup did not restore: ${it.message}" })
            }
            request.path == "/backup" -> PairingHttp.writeFile(out, "zettastream-backup.json", backup!!.export())
            request.method == "POST" -> {
                token = ""
                val lines = PairingHttp.form(request.body)["lines"].orEmpty()
                val count = runBlocking { onLines(lines) }
                PairingHttp.write(out, "200 OK", PairingHttp.done(count))
                onDone(count)
            }
            else -> PairingHttp.write(out, "200 OK", PairingHttp.page(token, currentText()))
        }
    }

    companion object {
        /** The app version and the device, at the top of the diagnostics page. */
        fun deviceInfo(): String =
            "ZettaStream ${io.github.stivengjekaj.zettastream.BuildConfig.VERSION_NAME}, ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}"

        const val PORT = 8765

        /** The most requests that the server handles at the same time. */
        const val MAX_OPEN = 4

        fun newToken(): String {
            val bytes = ByteArray(9)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }

        /** Finds the IPv4 address of this device on the local network. */
        fun lanAddress(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { if (it.name.startsWith("wlan") || it.name.startsWith("eth")) 0 else 1 }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
        }.getOrNull()
    }
}
