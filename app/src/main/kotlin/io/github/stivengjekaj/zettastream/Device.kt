package io.github.stivengjekaj.zettastream

import android.app.ActivityManager
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build

/** The two layouts of the app. */
enum class DeviceKind { Tv, Phone }

/** Selects the layout from the UI mode and the leanback feature. */
fun deviceKind(isTelevisionMode: Boolean, hasLeanback: Boolean): DeviceKind =
    if (isTelevisionMode || hasLeanback) DeviceKind.Tv else DeviceKind.Phone

/** Finds the layout for this device. */
fun Context.deviceKind(): DeviceKind {
    val uiMode = getSystemService(UiModeManager::class.java)?.currentModeType
    return deviceKind(
        isTelevisionMode = uiMode == Configuration.UI_MODE_TYPE_TELEVISION,
        hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
    )
}

/** The facts about the hardware that decide Lite mode. */
data class Hardware(val sdk: Int, val ramMb: Long, val lowRam: Boolean, val cores: Int)

/** Below this much memory, a device is weak. Many old TVs have 1 or 1.5 GB. */
const val WEAK_RAM_MB = 2_500L

/** True for an old or small device, such as an Android 7 TV with 1.5 GB of memory. */
fun isWeak(hardware: Hardware): Boolean =
    hardware.sdk < Build.VERSION_CODES.O || hardware.lowRam || hardware.ramMb < WEAK_RAM_MB

/** Reads the hardware of this device. */
fun Context.hardware(): Hardware {
    val activity = getSystemService(ActivityManager::class.java)
    val memory = ActivityManager.MemoryInfo().also { activity?.getMemoryInfo(it) }
    return Hardware(
        sdk = Build.VERSION.SDK_INT,
        ramMb = memory.totalMem / (1024 * 1024),
        lowRam = activity?.isLowRamDevice == true,
        cores = Runtime.getRuntime().availableProcessors(),
    )
}

/** One line about this device for Settings, such as "Android 7.0, 1.5 GB of memory, 4 cores, mt5658". */
fun Context.deviceSummary(): String {
    val h = hardware()
    val gb = String.format(java.util.Locale.US, "%.1f", h.ramMb / 1024.0)
    return "Android ${Build.VERSION.RELEASE}, $gb GB of memory, ${h.cores} cores, ${Build.HARDWARE}, ${Build.MANUFACTURER} ${Build.MODEL}"
}
