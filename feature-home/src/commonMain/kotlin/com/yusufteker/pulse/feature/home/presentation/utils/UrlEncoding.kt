package com.yusufteker.pulse.feature.home.presentation.utils

fun String.encodeUrlParameter(): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
    val builder = StringBuilder()
    for (char in this) {
        if (allowedChars.contains(char)) {
            builder.append(char)
        } else {
            val bytes = char.toString().encodeToByteArray()
            for (byte in bytes) {
                val hex = byte.toUByte().toString(16).uppercase()
                builder.append("%").append(if (hex.length == 1) "0$hex" else hex)
            }
        }
    }
    return builder.toString()
}
