package org.centrexcursionistalcoi.app.network

import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import java.net.SocketException
import java.net.UnknownHostException

actual fun isNoConnectionError(e: Throwable): Boolean {
    return when (e) {
        is UnresolvedAddressException -> true
        // SocketException covers ConnectException, and connections reset or aborted mid-request
        is UnknownHostException, is SocketTimeoutException, is SocketException -> true
        else -> false
    }
}
