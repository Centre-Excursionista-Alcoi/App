package org.centrexcursionistalcoi.app.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import org.koin.core.annotation.Singleton

@Singleton
actual class CredentialsStore(context: Context) {
    private val accountManager = AccountManager.get(context)

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
    }

    actual fun get(): SavedCredentials? {
        val account = accountManager.getAccountsByType(ACCOUNT_TYPE).firstOrNull() ?: return null
        val password = accountManager.getPassword(account) ?: return null
        return SavedCredentials(account.name, password.toCharArray())
    }

    actual fun clear() {
        accountManager.getAccountsByType(ACCOUNT_TYPE).forEach { accountManager.removeAccountExplicitly(it) }
    }
}
