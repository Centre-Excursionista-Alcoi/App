package org.centrexcursionistalcoi.app.platform

import com.diamondedge.logging.logging
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.*
import kotlinx.cinterop.ExperimentalForeignApi
import org.centrexcursionistalcoi.app.storage.fs.SystemDataPath
import org.koin.core.annotation.Singleton
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

@Singleton
actual class PlatformShareLogic : PlatformProvider {
    private val log = logging()

    actual override val isSupported: Boolean = true

    @OptIn(ExperimentalForeignApi::class)
    actual fun share(path: String, contentType: ContentType) {
        // The share sheet infers the file's type from its extension, so contentType doesn't need to be passed along.
        val filePath = SystemDataPath / path
        val url = NSURL.fileURLWithPath(filePath.toString())
        present(listOf(url))
    }

    actual fun share(text: String) {
        present(listOf(text))
    }

    private fun present(items: List<Any>) {
        val presenter = topViewController()
        if (presenter == null) {
            log.e { "No view controller available to present the share sheet from." }
            return
        }

        val activityViewController = UIActivityViewController(activityItems = items, applicationActivities = null).apply {
            // Required on iPad, where the share sheet is a popover anchored to a source view; without it the app crashes.
            popoverPresentationController?.sourceView = presenter.view
        }
        presenter.presentViewController(activityViewController, animated = true, completion = null)
    }

    private fun topViewController(): UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) {
            top = top.presentedViewController
        }
        return top
    }
}
