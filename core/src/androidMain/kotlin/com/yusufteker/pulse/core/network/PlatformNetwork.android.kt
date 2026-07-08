package com.yusufteker.pulse.core.network

import com.yusufteker.pulse.shared.isEmulator

/**
 * Returns the platform-specific base URL for the local backend server.
 * On Android emulator, 10.0.2.2 points to the host machine's localhost.
 */
actual fun getBaseUrl(): String {
    if (isEmulator()){
        return "http://10.0.2.2:8080/"
    }else{
          return  "https://pseudomilitaristic-brooks-feasibly.ngrok-free.dev/"
    }

}
