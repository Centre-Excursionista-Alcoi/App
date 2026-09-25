package org.centrexcursionistalcoi.app.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.Singleton

/** The account's user data key holding the refresh token. */
private const val KEY_REFRESH_TOKEN = "refresh_token"

@Singleton
actual class CredentialsStore(context: Context) {
    private val accountManager = AccountManager.get(context)

    actual val current: StateFlow<SavedAccount?>
        field = MutableStateFlow(readAccount())

    init {
        // minSdk 24 doesn't have the account-type-filtered overload (API 26+), so this fires for any account
        // type change; readAccount() re-checking ACCOUNT_TYPE specifically on every call is cheap enough that
        // filtering here isn't worth the version-gated code.
        accountManager.addOnAccountsUpdatedListener(
            { current.value = readAccount() },
            null,
            false,
        )
    }

    actual fun saveSession(email: String, refreshToken: String) {
        val account = Account(email, ACCOUNT_TYPE)
        // Only one saved account at a time: this app only ever has a single logged-in user locally.
        accountManager.getAccountsByType(ACCOUNT_TYPE)
            .filterNot { it.name == email }
            .forEach { accountManager.removeAccountExplicitly(it) }

        accountManager.addAccountExplicitly(account, null, null)
        accountManager.setUserData(account, KEY_REFRESH_TOKEN, refreshToken)
        // Forget the legacy password, if any: the session replaces it.
        accountManager.clearPassword(account)
        current.value = readAccount()
    }

    actual fun getSession(): SavedSession? {
        val account = findAccount() ?: return null
        val refreshToken = accountManager.getUserData(account, KEY_REFRESH_TOKEN) ?: return null
        return SavedSession(account.name, refreshToken)
    }

    actual fun getLegacyCredentials(): SavedCredentials? {
        val account = findAccount() ?: return null
        val password = accountManager.getPassword(account) ?: return null
        return SavedCredentials(account.name, password.toCharArray())
    }

    actual fun clear() {
        accountManager.getAccountsByType(ACCOUNT_TYPE).forEach { accountManager.removeAccountExplicitly(it) }
        current.value = readAccount()
    }

    private fun findAccount(): Account? = accountManager.getAccountsByType(ACCOUNT_TYPE).firstOrNull()

    private fun readAccount(): SavedAccount? = findAccount()?.let { SavedAccount(it.name) }
}
