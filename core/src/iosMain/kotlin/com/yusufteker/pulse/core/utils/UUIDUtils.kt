package com.yusufteker.pulse.core.utils

import platform.Foundation.NSUUID

actual fun generateUUID(): String {
    return NSUUID().UUIDString
}
