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
        .build()

    /** Downloads [url] and gives it to [read]. A gzip file is opened first. */
    suspend fun <T> stream(http: OkHttpClient, url: String, read: (InputStream) -> T): T = withContext(Dispatchers.IO) {
        http.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val input = response.body.byteStream().buffered()
            input.mark(2)
            val gzip = input.read() == 0x1f && input.read() == 0x8b
            input.reset()
            read(if (gzip) GZIPInputStream(input) else input)
        }
    }
}
