package com.yusufteker.pulse.core.utils

import kotlin.uuid.Uuid

fun generateUUID(): String {
    return Uuid.random().toString()
}
