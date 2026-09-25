package com.yusufteker.planora.core.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class IosShareManager : ShareManager {
    override fun shareText(text: String, title: String) {
        val activityViewController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
        
        val window = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
            ?.keyWindow
            ?: UIApplication.sharedApplication.keyWindow
            ?: (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)

        var topVC = window?.rootViewController
        while (topVC?.presentedViewController != null) {
            topVC = topVC.presentedViewController
        }

        if (topVC != null) {
            activityViewController.popoverPresentationController?.apply {
                sourceView = topVC.view
                sourceRect = platform.CoreGraphics.CGRectMake(0.0, 0.0, 1.0, 1.0)
            }
            topVC.presentViewController(
                viewControllerToPresent = activityViewController,
                animated = true,
                completion = null
            )
        }
    }
}
