package io.github.stivengjekaj.zettastream.player

/**
 * A look for the video. The overlays (scanlines, vignette, grain) are drawn on top of the
 * video and cost almost nothing. A colour change ([colour] is true) makes the player change
 * each pixel of each frame on the graphics chip, so it uses more power.
 */
enum class VideoFilter(
    val label: String,
    val colour: Boolean,
    val scanlines: Float = 0f,
    val vignette: Float = 0f,
    val grain: Float = 0f,
) {
    None("None", false),
    Crt("CRT", false, scanlines = 0.28f, vignette = 0.55f),
    Vhs("VHS", true, scanlines = 0.14f, vignette = 0.3f, grain = 0.10f),
    Vintage("Vintage film", true, vignette = 0.6f, grain = 0.14f),
    BlackWhite("Black and white", true, vignette = 0.25f),
}

/**
 * The colour matrices of the filters. A matrix has three rows of four numbers: the red, the
 * green, and the blue result, each from the red, green, and blue input and an added value.
 * Colours go from 0 to 1.
 */
object ColourMatrix {
    private val identity = listOf(floatArrayOf(1f, 0f, 0f, 0f), floatArrayOf(0f, 1f, 0f, 0f), floatArrayOf(0f, 0f, 1f, 0f))

    /** The weights of red, green, and blue in the brightness of a colour (Rec. 601). */
    private const val LR = 0.299f
    private const val LG = 0.587f
    private const val LB = 0.114f

    fun saturation(s: Float) = listOf(
        floatArrayOf(LR * (1 - s) + s, LG * (1 - s), LB * (1 - s), 0f),
        floatArrayOf(LR * (1 - s), LG * (1 - s) + s, LB * (1 - s), 0f),
        floatArrayOf(LR * (1 - s), LG * (1 - s), LB * (1 - s) + s, 0f),
    )

    /** [c] below 1 makes the darks lighter and the lights darker, around the middle grey. */
    fun contrast(c: Float) = identity.mapIndexed { i, row -> row.copyOf().also { it[i] = c; it[3] = 0.5f * (1 - c) } }

    /** Multiplies each colour, for example more red and less blue for a warm look. */
    fun tint(r: Float, g: Float, b: Float) = listOf(floatArrayOf(r, 0f, 0f, 0f), floatArrayOf(0f, g, 0f, 0f), floatArrayOf(0f, 0f, b, 0f))

    /** Makes black a dark grey and white a light grey, as an old print does. */
    fun fade(lift: Float, top: Float) = identity.mapIndexed { i, row -> row.copyOf().also { it[i] = top - lift; it[3] = lift } }

    val sepia = listOf(
        floatArrayOf(0.393f, 0.769f, 0.189f, 0f),
        floatArrayOf(0.349f, 0.686f, 0.168f, 0f),
        floatArrayOf(0.272f, 0.534f, 0.131f, 0f),
    )

    /** The result of [first], then [second]. */
    fun then(first: List<FloatArray>, second: List<FloatArray>): List<FloatArray> = second.map { row ->
        floatArrayOf(
            row[0] * first[0][0] + row[1] * first[1][0] + row[2] * first[2][0],
            row[0] * first[0][1] + row[1] * first[1][1] + row[2] * first[2][1],
            row[0] * first[0][2] + row[1] * first[1][2] + row[2] * first[2][2],
            row[0] * first[0][3] + row[1] * first[1][3] + row[2] * first[2][3] + row[3],
        )
    }

    /** A mix of [a] and [b]: 0 gives [a], 1 gives [b]. */
    fun mix(a: List<FloatArray>, b: List<FloatArray>, t: Float) = a.zip(b) { x, y -> FloatArray(4) { x[it] * (1 - t) + y[it] * t } }

    fun of(filter: VideoFilter): List<FloatArray>? = when (filter) {
        VideoFilter.None, VideoFilter.Crt -> null
        VideoFilter.Vhs -> then(then(then(saturation(0.75f), tint(1.05f, 1f, 0.9f)), contrast(0.88f)), fade(0.03f, 1f))
        VideoFilter.Vintage -> then(then(mix(identity, sepia, 0.85f), contrast(0.9f)), fade(0.06f, 0.95f))
        VideoFilter.BlackWhite -> then(saturation(0f), contrast(1.08f))
    }

    /** A colour through [matrix], kept from 0 to 1. */
    fun apply(matrix: List<FloatArray>, r: Float, g: Float, b: Float): FloatArray =
        FloatArray(3) { i -> (matrix[i][0] * r + matrix[i][1] * g + matrix[i][2] * b + matrix[i][3]).coerceIn(0f, 1f) }

    /** The 4 x 4 matrix of the player, in column order, as OpenGL reads it. Alpha does not change. */
    fun toGl(matrix: List<FloatArray>): FloatArray {
        val gl = FloatArray(16)
        for (row in 0 until 3) for (col in 0 until 4) gl[col * 4 + row] = matrix[row][col]
        gl[15] = 1f
        return gl
    }
}
