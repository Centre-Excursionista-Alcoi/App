package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.flow.Flow

actual object AccountManager {
    actual suspend fun get(): AccountAndPassword? {
        TODO()
    }

    actual fun flow(): Flow<AccountAndPassword?> {
        TODO()
    }

    actual suspend fun put(account: Account, password: String) {
        TODO()
    }

    actual  suspend fun logout() {
        TODO()
    }
}
