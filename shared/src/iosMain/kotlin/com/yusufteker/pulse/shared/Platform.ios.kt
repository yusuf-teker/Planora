package com.yusufteker.pulse.shared

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

actual fun getPlatformName(): String =
    UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

actual fun isEmulator(): Boolean {

    val env = NSProcessInfo.processInfo.environment
    return env["SIMULATOR_DEVICE_NAME"] != null || env["SIMULATOR_ROOT"] != null
}
