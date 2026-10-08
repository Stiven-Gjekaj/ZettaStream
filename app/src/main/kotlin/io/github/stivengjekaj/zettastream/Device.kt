package io.github.stivengjekaj.zettastream

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration

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
