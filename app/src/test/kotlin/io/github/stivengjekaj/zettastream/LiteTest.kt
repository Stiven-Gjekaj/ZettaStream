package io.github.stivengjekaj.zettastream

import io.github.stivengjekaj.zettastream.settings.LiteMode
import io.github.stivengjekaj.zettastream.ui.Lite
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteTest {
    private val strong = Hardware(sdk = 34, ramMb = 4_096, lowRam = false, cores = 8)

    @Test
    fun androidSevenIsWeak() {
        assertTrue(isWeak(strong.copy(sdk = 24)))
    }

    @Test
    fun smallMemoryIsWeak() {
        assertTrue(isWeak(strong.copy(ramMb = 1_536)))
    }

    @Test
    fun aLowRamDeviceIsWeak() {
        assertTrue(isWeak(strong.copy(lowRam = true)))
    }

    @Test
    fun aNewDeviceWithMemoryIsNotWeak() {
        assertFalse(isWeak(strong))
    }

    @Test
    fun autoFollowsTheHardware() {
        assertTrue(Lite.resolve(LiteMode.Auto, weak = true))
        assertFalse(Lite.resolve(LiteMode.Auto, weak = false))
    }

    @Test
    fun onAndOffOverrideTheHardware() {
        assertTrue(Lite.resolve(LiteMode.On, weak = false))
        assertFalse(Lite.resolve(LiteMode.Off, weak = true))
    }
}
