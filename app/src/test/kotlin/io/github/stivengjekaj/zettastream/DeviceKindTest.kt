package io.github.stivengjekaj.zettastream

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceKindTest {
    @Test
    fun televisionModeGivesTheTvLayout() {
        assertEquals(DeviceKind.Tv, deviceKind(isTelevisionMode = true, hasLeanback = false))
    }

    @Test
    fun leanbackFeatureGivesTheTvLayout() {
        assertEquals(DeviceKind.Tv, deviceKind(isTelevisionMode = false, hasLeanback = true))
    }

    @Test
    fun otherDevicesGiveThePhoneLayout() {
        assertEquals(DeviceKind.Phone, deviceKind(isTelevisionMode = false, hasLeanback = false))
    }
}
