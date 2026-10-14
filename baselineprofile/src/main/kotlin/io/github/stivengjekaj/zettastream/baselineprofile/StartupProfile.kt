package io.github.stivengjekaj.zettastream.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code that the app runs when it starts, when the viewer moves
 * through the rows of Home, and when a title opens. Android compiles this code
 * before the first start, so it does not run slowly the first times.
 */
@RunWith(AndroidJUnit4::class)
class StartupProfile {
    @get:Rule val rule = BaselineProfileRule()

    @Test
    fun record() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        pressHome()
        startActivityAndWait()
        closeNotice()
        device.wait(Until.hasObject(By.text("Watch now")), WAIT)
        // Move through the rows and along a row, as a remote control does.
        repeat(6) { device.pressDPadDown(); device.waitForIdle() }
        repeat(6) { device.pressDPadRight(); device.waitForIdle() }
        // Open a title, and go back to Home.
        device.pressDPadCenter()
        device.wait(Until.hasObject(By.textStartsWith("Watch")), WAIT)
        device.waitForIdle()
        device.pressBack()
        device.waitForIdle()
    }

    /** The notice about a VPN shows on the first start. */
    private fun MacrobenchmarkScope.closeNotice() {
        device.wait(Until.findObject(By.text("OK")), NOTICE_WAIT)?.click()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE = "io.github.stivengjekaj.zettastream.benchmark"
        const val WAIT = 15_000L
        const val NOTICE_WAIT = 3_000L
    }
}
