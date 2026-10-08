package io.github.stivengjekaj.zettastream.addon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PickSameSourceTest {
    private fun stream(url: String, name: String? = null, group: String? = null) =
        Stream(url = url, name = name, behaviorHints = group?.let { StreamHints(bingeGroup = it) })

    private val streams = listOf(
        stream("https://a/1", "Server A 720p", "a-720"),
        stream("https://b/1", "Server B 1080p", "b-1080"),
        stream("https://c/1", "Server C 1080p"),
    )

    @Test
    fun theBingeGroupComesFirst() {
        assertEquals("https://b/1", pickSameSource(streams, "b-1080", "Server A 720p")?.url)
    }

    @Test
    fun withNoBingeGroupTheSameNameIsTheMatch() {
        assertEquals("https://c/1", pickSameSource(streams, null, "Server C 1080p")?.url)
    }

    @Test
    fun withNoMatchTheFirstStreamIsTheChoice() {
        assertEquals("https://a/1", pickSameSource(streams, "gone", "gone")?.url)
        assertNull(pickSameSource(emptyList(), "a-720", null))
    }

    @Test
    fun theDataMattersMoreThanTheName() {
        val next = listOf(
            stream("https://x/720", "Server A", null).copy(title = "[Group] Show - 06 (720p) x264"),
            stream("https://x/1080", "Server B", null).copy(title = "[SubsPlease] Show - 06 (1080p) HEVC"),
        )
        val current = StreamInfo.parse("[SubsPlease] Show - 05 (1080p) HEVC")
        assertEquals("https://x/1080", pickSameSource(next, null, "Server A", current)?.url)
    }

    @Test
    fun theBingeGroupStillComesFirst() {
        val next = listOf(
            stream("https://x/1080", "B", "other").copy(title = "Show 1080p HEVC"),
            stream("https://x/720", "A", "mine").copy(title = "Show 720p"),
        )
        val current = StreamInfo.parse("Show 1080p HEVC")
        assertEquals("https://x/720", pickSameSource(next, "mine", null, current)?.url)
    }
}
