package org.centrexcursionistalcoi.app.auth

import com.russhwolf.settings.ExperimentalSettingsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.zip
import org.centrexcursionistalcoi.app.settings.settings

@OptIn(ExperimentalSettingsApi::class)
actual object AccountManager {
    private const val KEY_EMAIL = "email"
    private const val KEY_PASSWORD = "password"

    private val emailFlow = settings.getStringOrNullFlow(KEY_EMAIL)
    private val passwordFlow = settings.getStringOrNullFlow(KEY_PASSWORD)

    actual suspend fun get(): AccountAndPassword? {
        val email = settings.getStringOrNull(KEY_EMAIL) ?: return null
        val password = settings.getStringOrNull(KEY_PASSWORD) ?: return null
        return Account(email) to password
    }

    actual fun flow(): Flow<AccountAndPassword?> = emailFlow.zip(passwordFlow) { email, password ->
        if (email != null && password != null) {
            Account(email) to password
        } else {
            null
        }
    }

    actual suspend fun put(account: Account, password: String) {
        settings.putString(KEY_EMAIL, account.email)
        settings.putString(KEY_PASSWORD, password)
    }

    actual suspend fun logout() {
        settings.remove(KEY_EMAIL)
        settings.remove(KEY_PASSWORD)
    }
}
