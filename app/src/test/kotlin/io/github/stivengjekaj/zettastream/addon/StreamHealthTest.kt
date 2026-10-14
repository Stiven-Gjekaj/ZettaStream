package io.github.stivengjekaj.zettastream.addon

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StreamHealthTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun aFailedStreamIsRememberedForADay() = runBlocking {
        var now = 0L
        val health = StreamHealth(folder.newFile("health.json"), clock = { now })
        health.failed("a|https://x.test/1.m3u8", "a")
        assertTrue(health.failedRecently("a|https://x.test/1.m3u8"))
        now = StreamHealth.STREAM_MEMORY
        assertFalse(health.failedRecently("a|https://x.test/1.m3u8"))
    }

    @Test
    fun anAddonIsUnreliableAfterThreeFailuresAndAPlayClearsIt() = runBlocking {
        val health = StreamHealth(folder.newFile("health.json"))
        repeat(3) { health.failed("a|$it", "a") }
        assertTrue(health.unreliable("a"))
        health.played("a|9", "a")
        assertFalse(health.unreliable("a"))
    }

    @Test
    fun theMarksStayAfterARestart() = runBlocking {
        val file = folder.newFile("health.json")
        StreamHealth(file).failed("a|1", "a")
        assertTrue(StreamHealth(file).failedRecently("a|1"))
    }
}
