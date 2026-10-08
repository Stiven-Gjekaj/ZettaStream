package io.github.stivengjekaj.zettastream.addon

/** What the name and the title of a stream say about it. Each field is null when the text does not say. */
data class StreamInfo(
    val seeders: Int? = null,
    val resolution: Int? = null,
    val hdr: String? = null,
    val codec: String? = null,
    val source: String? = null,
    val dualAudio: Boolean = false,
    val sizeBytes: Long? = null,
    val group: String? = null,
) {
    /** The short labels that the list of streams shows. */
    val badges: List<String>
        get() = listOfNotNull(
            resolution?.let { if (it >= 2160) "4K" else "${it}p" },
            hdr,
            codec,
            source,
            if (dualAudio) "Dual audio" else null,
            sizeBytes?.let(::formatSize),
            seeders?.let { "$it seeders" },
        )

    companion object {
        /** Reads the text of a stream. Addons write the same facts in many forms. */
        fun of(stream: Stream): StreamInfo = parse(
            listOfNotNull(stream.name, stream.title, stream.description, stream.behaviorHints?.filename).joinToString("\n"),
        )

        private val seederPatterns = listOf(
            Regex("""👤\s*(\d+)"""),
            Regex("""👥\s*(\d+)"""),
            Regex("""🌱\s*(\d+)"""),
            Regex("""(?i)\bseed(?:er)?s?\s*[:=]?\s*(\d+)"""),
            Regex("""(?i)(\d+)\s*seed(?:er)?s?\b"""),
            Regex("""(?i)\bS\s*:\s*(\d+)\b"""),
        )
        private val resolutionPattern = Regex("""(?i)\b(2160|1440|1080|720|576|480|360)p\b""")
        private val fourK = Regex("""(?i)\b(4k|uhd)\b""")
        private val sizePattern = Regex("""(?i)(\d+(?:[.,]\d+)?)\s*(TB|GB|MB|GiB|MiB)\b""")
        private val groupPattern = Regex("""^\s*\[([^\]]{2,30})]""", RegexOption.MULTILINE)

        fun parse(text: String): StreamInfo {
            val seeders = seederPatterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1)?.toIntOrNull() }
            val resolution = resolutionPattern.findAll(text).mapNotNull { it.groupValues[1].toIntOrNull() }.maxOrNull()
                ?: if (fourK.containsMatchIn(text)) 2160 else null
            val hdr = when {
                Regex("""(?i)\b(dolby\s?vision|dovi|\bDV\b)""").containsMatchIn(text) -> "Dolby Vision"
                Regex("""(?i)\bHDR10\+""").containsMatchIn(text) -> "HDR10+"
                Regex("""(?i)\bHDR(10)?\b""").containsMatchIn(text) -> "HDR"
                else -> null
            }
            val codec = when {
                Regex("""(?i)\b(x265|h\.?265|hevc)\b""").containsMatchIn(text) -> "HEVC"
                Regex("""(?i)\bav1\b""").containsMatchIn(text) -> "AV1"
                Regex("""(?i)\b(x264|h\.?264|avc)\b""").containsMatchIn(text) -> "H.264"
                else -> null
            }
            val source = when {
                Regex("""(?i)\bremux\b""").containsMatchIn(text) -> "Remux"
                Regex("""(?i)\b(blu-?ray|bdrip|brrip|bd)\b""").containsMatchIn(text) -> "BluRay"
                Regex("""(?i)\bweb-?dl\b""").containsMatchIn(text) -> "WEB-DL"
                Regex("""(?i)\bweb-?rip\b""").containsMatchIn(text) -> "WEBRip"
                Regex("""(?i)\bhdtv\b""").containsMatchIn(text) -> "HDTV"
                Regex("""(?i)\b(cam|hdcam|camrip|telesync)\b""").containsMatchIn(text) -> "CAM"
                else -> null
            }
            val dual = Regex("""(?i)\b(dual[\s.-]?audio|multi[\s.-]?audio|eng(lish)?\s?dub|dubbed)\b""").containsMatchIn(text)
            val size = sizePattern.findAll(text).mapNotNull { m ->
                val n = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
                val unit = when (m.groupValues[2].uppercase()) {
                    "TB" -> 1e12; "GB" -> 1e9; "GIB" -> 1_073_741_824.0; "MB" -> 1e6; else -> 1_048_576.0
                }
                (n * unit).toLong()
            }.maxOrNull()
            val group = groupPattern.find(text)?.groupValues?.get(1)
            return StreamInfo(seeders, resolution, hdr, codec, source, dual, size, group)
        }

        fun formatSize(bytes: Long): String =
            if (bytes >= 1e9) "%.1f GB".format(bytes / 1e9) else "%d MB".format(bytes / 1_000_000)

        /**
         * Scores how much [candidate] is like [current], so that the next episode
         * plays from a source with the same data and not only the same name.
         */
        fun similarity(current: StreamInfo, candidate: StreamInfo): Int {
            var score = 0
            fun <T> same(a: T?, b: T?, points: Int, penalty: Int = 0) {
                if (a != null && b != null) score += if (a == b) points else -penalty
            }
            same(current.resolution, candidate.resolution, 40, 30)
            same(current.codec, candidate.codec, 15, 5)
            same(current.hdr, candidate.hdr, 10, 5)
            same(current.source, candidate.source, 10, 0)
            same(current.group, candidate.group, 30, 0)
            if (current.dualAudio == candidate.dualAudio) score += 20 else score -= 20
            return score
        }
    }
}
