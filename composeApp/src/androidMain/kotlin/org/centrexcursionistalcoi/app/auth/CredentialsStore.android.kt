package org.centrexcursionistalcoi.app.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton

@Singleton
actual class CredentialsStore(context: Context) {
    private val accountManager = AccountManager.get(context)

    actual val current: StateFlow<SavedCredentials?>
        field = MutableStateFlow(readCurrent())

    init {
        // minSdk 24 doesn't have the account-type-filtered overload (API 26+), so this fires for any account
        // type change; readCurrent() re-checking ACCOUNT_TYPE specifically on every call is cheap enough that
        // filtering here isn't worth the version-gated code.
        accountManager.addOnAccountsUpdatedListener(
            { current.value = readCurrent() },
            null,
            false,
        )
    }

    actual fun save(email: String, password: String) {
        val account = Account(email, ACCOUNT_TYPE)
        // Only one saved account at a time: this app only ever has a single logged-in user locally.
        accountManager.getAccountsByType(ACCOUNT_TYPE)
            .filterNot { it.name == email }
            .forEach { accountManager.removeAccountExplicitly(it) }

        if (!accountManager.addAccountExplicitly(account, password, null)) {
            // Account already existed (e.g. re-logging in as the same user): just update its password.
            accountManager.setPassword(account, password)
        }
        current.value = readCurrent()
    }

    actual fun get(): SavedCredentials? = readCurrent()

    actual fun clear() {
        accountManager.getAccountsByType(ACCOUNT_TYPE).forEach { accountManager.removeAccountExplicitly(it) }
        current.value = readCurrent()
    }

    private fun readCurrent(): SavedCredentials? {
        val account = accountManager.getAccountsByType(ACCOUNT_TYPE).firstOrNull() ?: return null
        val password = accountManager.getPassword(account) ?: return null
        return SavedCredentials(account.name, password.toCharArray())
    }
}
