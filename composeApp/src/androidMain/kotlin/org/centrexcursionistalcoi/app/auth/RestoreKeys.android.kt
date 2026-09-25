package org.centrexcursionistalcoi.app.auth

import androidx.credentials.exceptions.NoCredentialException
import com.diamondedge.logging.logging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.centrexcursionistalcoi.app.data.TokenResponse
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.koin.core.annotation.Singleton

@Singleton
actual class RestoreKeys(
    private val credentialManagerRepository: CredentialManagerRepository,
    dispatcherProvider: DispatcherProvider,
) {
    private val log = logging()

    /**
     * Serializes every Credential Manager operation, so e.g. a restore key being cleared on logout can't race a
     * restore key still being created from the previous login.
     */
    private val lock = Mutex()

    /**
     * Work that shouldn't block [create]/[clear]'s callers. [SupervisorJob]: one failed job must not cancel the
     * scope for every later one.
     */
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    // Not fatal if it fails: this feature will just be missing for this user.
    actual fun create() = launch("create a restore key") { credentialManagerRepository.create() }

    actual fun clear() = launch("clear the restore key") { credentialManagerRepository.clear() }

    actual suspend fun redeem(): TokenResponse? = lock.withLock {
        try {
            credentialManagerRepository.recover().also {
                log.d { "Session restored with the Credential Manager's restore key." }
            }
        } catch (_: NoCredentialException) {
            log.d { "No restore key available in the Credential Manager." }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Failed to restore the session with the Credential Manager's restore key." }
            null
        }
    }

    private fun launch(description: String, block: suspend () -> Unit) {
        scope.launch {
            lock.withLock {
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
}
