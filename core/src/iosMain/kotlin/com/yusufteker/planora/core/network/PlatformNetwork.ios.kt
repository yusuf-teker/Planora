package com.yusufteker.planora.core.network

import com.yusufteker.planora.shared.isEmulator

/**
 * Returns the platform-specific base URL for the local backend server.
 * On iOS simulator, 127.0.0.1 points to the Mac's localhost.
 * (If testing on a physical iOS device, this should be the Mac's actual IP address on the local Wi-Fi, e.g., 192.168.x.x)
 */
actual fun getBaseUrl(): String {

    if (isEmulator()){
        return "https://pulse-7b4z.onrender.com"//"http://127.0.0.1:8080/"
    }else{
        return  "https://pulse-7b4z.onrender.com"//"https://pseudomilitaristic-brooks-feasibly.ngrok-free.dev"
    }
}
