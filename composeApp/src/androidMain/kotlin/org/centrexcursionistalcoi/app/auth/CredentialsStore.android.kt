package org.centrexcursionistalcoi.app.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.credentials.exceptions.NoCredentialException
import com.diamondedge.logging.logging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    dispatcherProvider: DispatcherProvider,
) {
    private val accountManager = AccountManager.get(context)
    private val log = logging()

    /**
     * Serializes every Credential Manager operation, so e.g. a restore key being cleared on logout can't race a
     * restore key still being created from the previous login.
     */
    private val credentialManagerLock = Mutex()

    /**
     * Credential Manager work that shouldn't block [save]/[clear]'s callers. [SupervisorJob]: one failed job must
     * not cancel the scope for every later one.
     */
    private val credentialManagerScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    actual val current: StateFlow<SavedCredentials?>
        field = MutableStateFlow(readCurrent())

    init {
        // TODO: At some point we should not rely on AccountManager for storing whether the user is not logged in or not
        // The ideal thing would be to store the account somehow internally in our database (+CredentialManager), and then use that to determine if the user is logged in or not

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

        // Also store a restore key, so the session can be recovered on a new device (see restoreSession()).
        // Not fatal if it fails: this feature will just be missing for this user.
        launchCredentialManagerOperation("create a restore key") { credentialManagerRepository.create() }
    }

    actual suspend fun get(): SavedCredentials? = readCurrent()

    actual suspend fun clear() {
        accountManager.getAccountsByType(ACCOUNT_TYPE).forEach { accountManager.removeAccountExplicitly(it) }
        current.value = readCurrent()

        launchCredentialManagerOperation("clear the restore key") { credentialManagerRepository.clear() }
    }

    actual suspend fun restoreSession(): Boolean = credentialManagerLock.withLock {
        try {
            credentialManagerRepository.recover()
            log.d { "Session restored with the Credential Manager's restore key." }
            true
        } catch (_: NoCredentialException) {
            log.d { "No restore key available in the Credential Manager." }
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Failed to restore the session with the Credential Manager's restore key." }
            false
        }
    }

    private fun launchCredentialManagerOperation(description: String, block: suspend () -> Unit) {
        credentialManagerScope.launch {
            credentialManagerLock.withLock {
                try {
                    block()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(e) { "Failed to $description in the Credential Manager." }
                }
            }
        }
    }

    private fun readCurrent(): SavedCredentials? {
        val account = accountManager.getAccountsByType(ACCOUNT_TYPE).firstOrNull() ?: return null
        val password = accountManager.getPassword(account) ?: return null
        return SavedCredentials(account.name, password.toCharArray())
    }
}
