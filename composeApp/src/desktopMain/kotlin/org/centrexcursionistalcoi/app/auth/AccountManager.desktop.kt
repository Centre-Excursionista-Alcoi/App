package org.centrexcursionistalcoi.app.auth

import com.russhwolf.settings.ExperimentalSettingsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.settings.settings

@OptIn(ExperimentalSettingsApi::class)
actual object AccountManager {
    private const val KEY_EMAIL = "email"

    actual suspend fun get(): Account? {
        val email = settings.getStringOrNull(KEY_EMAIL) ?: return null
        return Account(email)
    }

    actual fun flow(): Flow<Account?> = settings.getStringOrNullFlow(KEY_EMAIL).map { it?.let { Account(it) } }

    actual suspend fun put(account: Account, password: String) {
        settings.putString(KEY_EMAIL, account.email)
    }

    actual suspend fun logout() {
        settings.remove(KEY_EMAIL)
    }
}
