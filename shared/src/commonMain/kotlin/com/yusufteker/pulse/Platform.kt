package com.yusufteker.pulse

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform