package `in`.gym.trak.studio.utils

import com.ananta.gym.utils.topViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.URLQueryAllowedCharacterSet
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.stringByAddingPercentEncodingWithAllowedCharacters
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIUserInterfaceIdiomPad
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class)
private fun presentShareSheet(controller: UIActivityViewController) {
    val presenter = topViewController() ?: return

    if (presenter.traitCollection.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        controller.popoverPresentationController?.sourceView = presenter.view
        controller.popoverPresentationController?.sourceRect = presenter.view.bounds
    }

    presenter.presentViewController(controller, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformShare(payload: SharePayload) {
    val text = payload.text.trim()
    if (text.isBlank()) return

    if (payload.channel == ShareChannel.WhatsApp) {
        val encoded = (text as NSString).stringByAddingPercentEncodingWithAllowedCharacters(
            allowedCharacters = NSCharacterSet.URLQueryAllowedCharacterSet
        ) ?: text
        val waUrl = NSURL.URLWithString("https://wa.me/?text=$encoded")
        if (waUrl != null && UIApplication.sharedApplication.canOpenURL(waUrl)) {
            UIApplication.sharedApplication.openURL(waUrl)
            return
        }
    }

    val activityItems = listOf(text)
    val controller = UIActivityViewController(
        activityItems = activityItems,
        applicationActivities = null,
    )
    presentShareSheet(controller)
}

@OptIn(ExperimentalForeignApi::class)
actual fun platformShareImage(payload: ShareImagePayload) {
    if (payload.pngBytes.isEmpty()) return

    val data = payload.pngBytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), payload.pngBytes.size.toULong())
    } ?: return

    val image = UIImage.imageWithData(data) ?: return
    val controller = UIActivityViewController(
        activityItems = listOf(image),
        applicationActivities = null,
    )
    presentShareSheet(controller)
}
