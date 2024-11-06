package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.flow.Flow

typealias AccountAndPassword = Pair<Account, String>

expect object AccountManager {
    suspend fun get(): AccountAndPassword?

    fun flow(): Flow<AccountAndPassword?>

    suspend fun put(account: Account, password: String)

    suspend fun logout()
}
