package org.centrexcursionistalcoi.app.network

import io.ktor.network.sockets.SocketTimeoutException
import io.ktor.util.network.UnresolvedAddressException
import java.net.ConnectException
import java.net.UnknownHostException

actual fun isNoConnectionError(e: Throwable): Boolean {
    return when (e) {
        is UnresolvedAddressException -> true
        is UnknownHostException, is SocketTimeoutException, is ConnectException -> true
        else -> false
    }
}
