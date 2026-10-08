package io.github.stivengjekaj.zettastream.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TypeKeyTest {
    @Test
    fun typesThatMeanTheSameThingHaveOneKey() {
        assertEquals(listOf("sport"), listOf("sport", "sports", "Sports", "events").map(::typeKey).distinct())
        assertEquals(listOf("tv"), listOf("tv", "channel", "channels").map(::typeKey).distinct())
        assertEquals("anime", typeKey("anime"))
    }

    @Test
    fun eachKeyHasOneLabel() {
        assertEquals("Sports", typeLabel("sports"))
        assertEquals("Sports", typeLabel("sport"))
        assertEquals("TV channels", typeLabel("channel"))
        assertEquals("All", typeLabel(null))
    }
}
