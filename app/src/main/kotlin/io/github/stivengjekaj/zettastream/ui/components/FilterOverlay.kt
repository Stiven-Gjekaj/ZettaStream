package io.github.stivengjekaj.zettastream.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import io.github.stivengjekaj.zettastream.player.VideoFilter
import kotlinx.coroutines.delay
import kotlin.random.Random

/** The time between two images of the grain. A few images a second look like film and cost little. */
private const val GRAIN_INTERVAL = 125L
private const val GRAIN_SIZE = 128

/**
 * The scanlines, the dark corners, and the grain of a filter, drawn on top of the video.
 * The scanlines and the corners are drawn one time. Only the grain moves.
 */
@Composable
fun FilterOverlay(filter: VideoFilter, modifier: Modifier = Modifier) {
    if (filter.scanlines > 0f || filter.vignette > 0f) {
        Canvas(
            modifier.fillMaxSize().drawWithCache {
                val lines = Color.Black.copy(alpha = filter.scanlines)
                val corners = Brush.radialGradient(
                    0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = filter.vignette),
                    center = Offset(size.width / 2, size.height / 2), radius = size.maxDimension * 0.62f,
                )
                onDrawBehind {
                    if (filter.scanlines > 0f) {
                        // One dark line in each three lines of pixels, as on a tube screen.
                        var y = 0f
                        while (y < size.height) {
                            drawLine(lines, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                            y += 3f
                        }
                    }
                    if (filter.vignette > 0f) drawRect(corners)
                }
            },
        ) {}
    }
    if (filter.grain > 0f) Grain(filter.grain, modifier)
}

@Composable
private fun Grain(strength: Float, modifier: Modifier) {
    val brush = remember(strength) {
        val random = Random(7)
        val pixels = IntArray(GRAIN_SIZE * GRAIN_SIZE) {
            val v = random.nextInt(256)
            val a = (random.nextFloat() * strength * 255).toInt()
            (a shl 24) or (v shl 16) or (v shl 8) or v
        }
        val bitmap = Bitmap.createBitmap(pixels, GRAIN_SIZE, GRAIN_SIZE, Bitmap.Config.ARGB_8888)
        ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
    // The same grain moves to a new place a few times a second, so it lives as film grain does.
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(GRAIN_INTERVAL); step++ } }
    Canvas(modifier.fillMaxSize()) {
        val r = Random(step)
        translate(-r.nextInt(GRAIN_SIZE).toFloat(), -r.nextInt(GRAIN_SIZE).toFloat()) {
            drawRect(brush, topLeft = Offset.Zero, size = size.copy(width = size.width + GRAIN_SIZE, height = size.height + GRAIN_SIZE))
        }
    }
}
