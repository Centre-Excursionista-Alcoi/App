package org.centrexcursionistalcoi.app.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import java.io.Closeable
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * The uploads received by a call (see [org.centrexcursionistalcoi.app.request.FileRequestData]), closed once the
 * call has been handled, however it ends. Available in the coroutine context of every call.
 */
class CallUploads : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<CallUploads> {
        private val logger = LoggerFactory.getLogger(CallUploads::class.java)
    }

    private val uploads = ConcurrentLinkedQueue<Closeable>()

    fun register(upload: Closeable) {
        uploads += upload
    }

    fun closeAll() {
        while (true) {
            val upload = uploads.poll() ?: break
            try {
                upload.close()
            } catch (e: Exception) {
                logger.error("Could not release an upload", e)
            }
        }
    }
}

/**
 * Releases the uploads of every call once it has been handled, so that their temporary files never stay behind,
 * even if the handler fails or returns before storing them.
 */
fun Application.configureUploadsCleanup() {
    intercept(ApplicationCallPipeline.Setup) {
        val uploads = CallUploads()
        try {
            withContext(uploads) { proceed() }
        } finally {
            uploads.closeAll()
        }
    }
}
