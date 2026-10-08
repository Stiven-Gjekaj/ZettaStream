package io.github.stivengjekaj.zettastream.net

import io.github.stivengjekaj.zettastream.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object Http {
    const val USER_AGENT = "ZettaStream/${BuildConfig.VERSION_NAME}"

    /**
     * A request with this header keeps its answer in the cache for the given number of seconds.
     * The header does not go to the server. It applies only when the server sets no time itself.
     */
    const val KEEP_FOR = "X-ZettaStream-Keep-For"

    fun client(cacheDir: File): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .cache(okhttp3.Cache(File(cacheDir, "http"), 50L * 1024 * 1024))
        .addInterceptor { chain ->
            val request = chain.request()
            chain.proceed(
                if (request.header("User-Agent") == null) request.newBuilder().header("User-Agent", USER_AGENT).build()
                else request,
            )
        }
        .addNetworkInterceptor { chain ->
            val request = chain.request()
            val keep = request.header(KEEP_FOR)
            val response = chain.proceed(request.newBuilder().removeHeader(KEEP_FOR).build())
            val set = response.header("Cache-Control").orEmpty()
            if (keep == null || !response.isSuccessful || "max-age" in set || response.header("Expires") != null) response
            else response.newBuilder().header("Cache-Control", "max-age=$keep").removeHeader("Pragma").build()
        }
        .build()

    /** Downloads [url] and gives it to [read]. A gzip file is opened first. [keepForSeconds] keeps the file in the cache. */
    suspend fun <T> stream(http: OkHttpClient, url: String, keepForSeconds: Long? = null, read: (InputStream) -> T): T = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).apply { keepForSeconds?.let { header(KEEP_FOR, it.toString()) } }.build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val input = response.body.byteStream().buffered()
            input.mark(2)
            val gzip = input.read() == 0x1f && input.read() == 0x8b
            input.reset()
            read(if (gzip) GZIPInputStream(input) else input)
        }
    }
}
