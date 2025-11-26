package org.centrexcursionistalcoi.app.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.NSURLErrorNotConnectedToInternet
import platform.Foundation.NSURLErrorTimedOut

actual fun isNoConnectionError(e: Throwable): Boolean {
    val nsError = (e as? DarwinHttpRequestException)?.origin
    if (nsError != null) {
        return nsError.domain == NSURLErrorDomain && (nsError.code == NSURLErrorNotConnectedToInternet || nsError.code == NSURLErrorTimedOut || nsError.code == -1004L)
    }
    return when (e) {
        is UnresolvedAddressException -> true
        is SocketTimeoutException -> true
        else -> false
    }
}