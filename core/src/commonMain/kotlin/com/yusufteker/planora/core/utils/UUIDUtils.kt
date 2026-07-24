package com.yusufteker.planora.core.utils

import kotlin.uuid.Uuid

fun generateUUID(): String {
    return Uuid.random().toString()
}
