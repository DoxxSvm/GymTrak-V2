package com.ananta.gym.utils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
internal fun activeWindow(): UIWindow? {
    val application = UIApplication.sharedApplication
    application.keyWindow?.let { return it }

    val windows = application.windows
    for (index in windows.size - 1 downTo 0) {
        val window = windows[index] as? UIWindow ?: continue
        if (!window.isHidden() && window.alpha > 0.0) {
            return window
        }
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
internal fun topViewController(): UIViewController? {
    val root = activeWindow()?.rootViewController ?: return null
    return topMostViewController(root)
}

@OptIn(ExperimentalForeignApi::class)
private fun topMostViewController(controller: UIViewController): UIViewController {
    val presented = controller.presentedViewController ?: return controller
    return topMostViewController(presented)
}
