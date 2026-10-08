package io.github.stivengjekaj.zettastream.pairing

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
    const val MAX_BODY = 256 * 1024

    fun read(input: InputStream): HttpRequest? {
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
        if (length > MAX_BODY) return null
        val body = ByteArray(length)
        var read = 0
        while (read < length) {
            val n = input.read(body, read, length - read)
            if (n < 0) break
            read += n
        }
        val target = parts[1]
        return HttpRequest(
            method = parts[0],
            path = target.substringBefore('?'),
            query = form(target.substringAfter('?', "")),
            body = String(body, 0, read, Charsets.UTF_8),
        )
    }

    fun form(text: String): Map<String, String> = text.split('&').filter { it.isNotEmpty() }.associate {
        URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
    }

    fun write(output: OutputStream, status: String, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        output.write(
            ("HTTP/1.1 $status\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n").toByteArray(),
        )
        output.write(bytes)
        output.flush()
    }

    fun page(token: String, current: String, message: String? = null): String = """
        <!doctype html><html><head><meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>ZettaStream sources</title>
        <style>
          body{margin:0;padding:16px;background:#000;color:#f2f2f2;font-family:ui-monospace,'JetBrains Mono',Menlo,monospace}
          h1{font-size:20px;margin:8px 0 4px;letter-spacing:-0.5px}p{color:#8c8c8c;font-size:13px}
          textarea{width:100%;box-sizing:border-box;height:55vh;background:#0e0e0e;color:#f2f2f2;border:1px solid #2e2e2e;border-radius:8px;padding:10px;font:12px ui-monospace,monospace}
          button{margin-top:12px;width:100%;padding:14px;border:0;border-radius:8px;background:#fff;color:#000;font:600 15px ui-monospace,monospace}
          .msg{padding:12px;border-radius:8px;background:#181818;color:#f2f2f2}
        </style></head><body>
        <h1>ZettaStream sources</h1>
        ${if (message != null) "<p class=\"msg\">${escape(message)}</p>" else ""}
        <p>Paste one URL on each line: a Stremio addon <code>manifest.json</code>, an M3U playlist, or an XMLTV guide.
        This list replaces the list on the TV.</p>
        <form method="post" action="/?t=${escape(token)}">
        <textarea name="lines" autocapitalize="off" autocorrect="off" spellcheck="false">${escape(current)}</textarea>
        <button type="submit">Send to the TV</button></form></body></html>
    """.trimIndent()

    fun done(count: Int): String = """
        <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <style>body{margin:0;padding:24px;background:#000;color:#f2f2f2;font-family:ui-monospace,monospace}</style></head>
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
) {
    private var socket: ServerSocket? = null
    private var job: Job? = null
    @Volatile private var token: String = newToken()

    /** Starts the server and gives the URL to show in the QR code. */
    fun start(): String? {
        val address = lanAddress() ?: return null
        val server = runCatching { ServerSocket(PORT) }.getOrElse { ServerSocket(0) }
        socket = server
        token = newToken()
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val client = runCatching { server.accept() }.getOrNull() ?: break
                launch { handle(client) }
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
        val request = PairingHttp.read(BufferedInputStream(s.getInputStream())) ?: return
        val out = s.getOutputStream()
        val valid = token.isNotEmpty() && request.query["t"] == token
        when {
            request.path != "/" -> PairingHttp.write(out, "404 Not Found", "Not found")
            !valid -> PairingHttp.write(out, "403 Forbidden", "This code is old. Scan the new QR code on the TV.")
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
        const val PORT = 8765

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
