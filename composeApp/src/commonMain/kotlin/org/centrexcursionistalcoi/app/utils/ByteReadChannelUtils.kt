package org.centrexcursionistalcoi.app.utils

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.close
import kotlinx.io.InternalIoApi
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier

@OptIn(InternalAPI::class, InternalIoApi::class)
suspend fun ByteReadChannel.copyTo(channel: ByteWriteChannel, progressNotifier: ProgressNotifier): Long {
    var result = 0L
    val size = readBuffer.buffer.size
    try {
        while (!isClosedForRead) {
            result += readBuffer.transferTo(channel.writeBuffer)
            progressNotifier(Progress.LocalFSRead(result, size))
            channel.flush()
            awaitContent()
        }
    } catch (cause: Throwable) {
        cancel(cause)
        channel.close(cause)
        throw cause
    } finally {
        channel.flush()
    }

    return result
}
