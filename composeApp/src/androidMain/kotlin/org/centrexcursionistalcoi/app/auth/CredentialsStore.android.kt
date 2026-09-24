package org.centrexcursionistalcoi.app.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import com.diamondedge.logging.logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.koin.core.annotation.Singleton

@Singleton
actual class CredentialsStore(
    context: Context,
    private val credentialManagerRepository: CredentialManagerRepository,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val accountManager = AccountManager.get(context)
    private val credentialFetchLock = Mutex()
    private val log = logging()

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

    actual suspend fun save(email: String, password: String) {
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

        // asynchronously store the credential in the Credential Manager, if available
        CoroutineScope(dispatcherProvider.io).launch {
            try {
                credentialFetchLock.lock()
                credentialManagerRepository.create()
            } catch (e: IllegalStateException) {
                // E2EE is not available and the credential cannot be created without cloud backup
                // This is not a fatal error, this feature will just be missing for this user. Log it and continue.
                log.error("Failed to create credential in Credential Manager", e)
            } finally {
                credentialFetchLock.unlock()
            }
        }
    }

    actual suspend fun get(): SavedCredentials? = readCurrent()

    actual suspend fun clear() {
        accountManager.getAccountsByType(ACCOUNT_TYPE).forEach { accountManager.removeAccountExplicitly(it) }
        current.value = readCurrent()

        // asynchronously clear the credential in the Credential Manager, if available
        CoroutineScope(dispatcherProvider.io).launch {
            try {
                credentialFetchLock.lock()
                credentialManagerRepository.clear()
            } finally {
                credentialFetchLock.unlock()
            }
        }
    }

    private suspend fun readCurrent(): SavedCredentials? {
        val account = accountManager.getAccountsByType(ACCOUNT_TYPE).firstOrNull()
        if (account == null) {
            // No account found locally, let's try to recover it from the Credential Manager if available
            credentialFetchLock.withLock {
                credentialManagerRepository.recover()
            }
            // if there are no errors, it means re-authentication was successful
            throw AuthenticationAlreadyHandledByCredentialManagerException()
        }
        val password = accountManager.getPassword(account) ?: return null
        return SavedCredentials(account.name, password.toCharArray())
    }
}
