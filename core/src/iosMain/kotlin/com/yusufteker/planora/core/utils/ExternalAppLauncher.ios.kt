package com.yusufteker.planora.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosExternalAppLauncher : ExternalAppLauncher {
    override fun isAppInstalled(app: ExternalApp): Boolean {
        val url = NSURL.URLWithString(app.scheme) ?: return false
        return UIApplication.sharedApplication.canOpenURL(url)
    }

    override fun openApp(app: ExternalApp): Boolean {
        val url = NSURL.URLWithString(app.scheme) ?: return false
        if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
            return true
        }
        return false
    }
}

@Composable
actual fun rememberExternalAppLauncher(): ExternalAppLauncher {
    return remember {
        IosExternalAppLauncher()
    }
}
