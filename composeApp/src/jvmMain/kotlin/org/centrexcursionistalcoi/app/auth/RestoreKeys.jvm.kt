package org.centrexcursionistalcoi.app.auth

import org.centrexcursionistalcoi.app.data.TokenResponse
import org.koin.core.annotation.Singleton

// No restore keys on desktop.
@Singleton
actual class RestoreKeys {
    actual fun create() {}
    actual fun clear() {}
    actual suspend fun redeem(): TokenResponse? = null
}
