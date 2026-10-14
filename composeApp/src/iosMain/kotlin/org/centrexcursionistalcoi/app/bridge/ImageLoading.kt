@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package org.centrexcursionistalcoi.app.bridge

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.centrexcursionistalcoi.app.data.ImageFileContainer
import org.centrexcursionistalcoi.app.data.ImageFileListContainer
import org.centrexcursionistalcoi.app.data.imageFile
import platform.Foundation.NSData
import platform.Foundation.create
import kotlin.uuid.Uuid

/**
 * The images of events, posts, inventory and the like, for the native UI: each one is downloaded the first time
 * (and kept on the device) by the same code the Compose UI uses, and handed over as `Data`.
 */
object ImageLoading {
    /** The image of [container], or `null` if it has none, or it couldn't be loaded. */
    suspend fun image(container: ImageFileContainer): NSData? = try {
        container.imageFile()?.toNSData()
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        null
    }

    /** The image with [id] of [container], or `null` if it couldn't be loaded. */
    suspend fun image(container: ImageFileListContainer, id: Uuid): NSData? = try {
        container.imageFile(id)?.toNSData()
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        null
    }
}

private fun ByteArray.toNSData(): NSData? {
    if (isEmpty()) return null
    return usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
}
