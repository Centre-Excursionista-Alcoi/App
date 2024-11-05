package org.centrexcursionistalcoi.app.auth

import kotlinx.coroutines.flow.Flow

expect object AccountManager {
    suspend fun get(): Account?

    fun flow(): Flow<Account?>

    suspend fun put(account: Account, password: String)
}
