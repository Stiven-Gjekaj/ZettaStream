package io.github.stivengjekaj.zettastream.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures a cold start and a move through the rows of Home, with no compiled
 * code and with the startup profile. The difference is what the profile gives
 * in the first days after an install or an update.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun startWithoutProfile() = start(CompilationMode.None())

    @Test fun startWithProfile() = start(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun start(mode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        compilationMode = mode,
        startupMode = StartupMode.COLD,
        iterations = 8,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Watch now")), WAIT)
        repeat(6) { device.pressDPadDown(); device.waitForIdle() }
        repeat(6) { device.pressDPadRight(); device.waitForIdle() }
    }

    private companion object {
        const val PACKAGE = "io.github.stivengjekaj.zettastream.benchmark"
        const val WAIT = 15_000L
    }
}
