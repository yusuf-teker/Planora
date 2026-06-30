package com.yusufteker.pulse.feature.home.presentation.create_task

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual fun getCurrentTimeMs(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
