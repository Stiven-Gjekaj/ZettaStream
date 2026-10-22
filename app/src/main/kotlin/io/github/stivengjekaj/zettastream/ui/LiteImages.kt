package io.github.stivengjekaj.zettastream.ui

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import coil3.request.allowRgb565

/**
 * In Lite mode each image uses 2 bytes for each pixel, not 4, when it has no transparency.
 * A weak TV then decodes, keeps, and uploads half the data for each poster.
 */
class LiteImages : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        if (!Lite.on) return chain.proceed()
        return chain.withRequest(chain.request.newBuilder().allowRgb565(true).build()).proceed()
    }
}
