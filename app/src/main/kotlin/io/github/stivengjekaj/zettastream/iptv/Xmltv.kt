package io.github.stivengjekaj.zettastream.iptv

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** One programme in the TV guide. Times are in milliseconds since 1970. */
data class Programme(
    val channelId: String,
    val title: String,
    val start: Long,
    val stop: Long,
    val description: String? = null,
)

/** The programmes of each channel, in the order of their start times. */
class Guide(private val byChannel: Map<String, List<Programme>>) {
    fun programmes(channelId: String?): List<Programme> =
        channelId?.let { byChannel[it] }.orEmpty()

    /** Finds the programme that is on now and the one after it. */
    fun nowAndNext(channelId: String?, now: Long): Pair<Programme?, Programme?> {
        val list = programmes(channelId)
        val index = list.indexOfFirst { it.stop > now }
        if (index < 0) return null to null
        val first = list[index]
        return if (first.start <= now) first to list.getOrNull(index + 1) else null to first
    }

    operator fun plus(other: Guide): Guide =
        Guide((byChannel.keys + other.byChannel.keys).associateWith { key ->
            (byChannel[key].orEmpty() + other.byChannel[key].orEmpty()).sortedBy { it.start }
        })

    companion object {
        val Empty = Guide(emptyMap())
    }
}

/** Reads a TV guide in the XMLTV form. */
object Xmltv {
    private val format = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    /**
     * Reads [input]. A guide file can be very large, so this keeps only the
     * channels in [channelIds] and only the programmes between [from] and [to].
     */
    fun parse(input: InputStream, channelIds: Set<String>?, from: Long, to: Long): Guide {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(input, null)
        val result = HashMap<String, MutableList<Programme>>()

        var channel: String? = null
        var start = 0L
        var stop = 0L
        var title: String? = null
        var description: String? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "programme" -> {
                        channel = parser.getAttributeValue(null, "channel")
                        start = parseTime(parser.getAttributeValue(null, "start"))
                        stop = parseTime(parser.getAttributeValue(null, "stop"))
                        title = null
                        description = null
                    }
                    "title" -> if (channel != null && title == null) title = parser.nextText().trim()
                    "desc" -> if (channel != null && description == null) description = parser.nextText().trim()
                }
            } else if (event == XmlPullParser.END_TAG && parser.name == "programme") {
                val id = channel
                if (id != null && (channelIds == null || id in channelIds) && stop > from && start < to && start > 0) {
                    result.getOrPut(id) { mutableListOf() } += Programme(id, title.orEmpty(), start, stop, description)
                }
                channel = null
            }
            event = parser.next()
        }
        return Guide(result.mapValues { (_, list) -> list.sortedBy { it.start } })
    }

    /** Reads a time such as `20261008120000 +0200`. With no offset, it uses UTC. */
    fun parseTime(value: String?): Long {
        if (value == null || value.length < 14) return 0
        return runCatching {
            val local = LocalDateTime.parse(value.substring(0, 14), format)
            val offsetText = value.substring(14).trim()
            val offset = if (offsetText.isEmpty()) ZoneOffset.UTC else ZoneOffset.of(
                if (offsetText.length == 5) offsetText.substring(0, 3) + ":" + offsetText.substring(3) else offsetText,
            )
            local.toInstant(offset).toEpochMilli()
        }.getOrDefault(0)
    }
}
