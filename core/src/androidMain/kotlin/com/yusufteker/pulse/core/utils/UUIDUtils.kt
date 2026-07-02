package com.yusufteker.pulse.core.utils

import java.util.UUID

actual fun generateUUID(): String {
    return UUID.randomUUID().toString()
}
