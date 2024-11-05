package org.centrexcursionistalcoi.app.auth

actual object AccountManager {
    actual suspend fun get(): Account? {
        TODO("Not yet implemented")
    }

    actual suspend fun put(account: Account, password: String) {
    }

}