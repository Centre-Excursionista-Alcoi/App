package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.auth.PasskeyException
import org.centrexcursionistalcoi.app.auth.Passkeys
import org.centrexcursionistalcoi.app.auth.SavedCredential
import org.centrexcursionistalcoi.app.data.Reauthentication
import org.centrexcursionistalcoi.app.data.SecurityInfo
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.SecurityRemoteRepository
import org.koin.core.annotation.KoinViewModel

/** A change that needs the user to confirm it's them first, see [Reauthentication]. */
sealed interface SecurityChange {
    data class RemovePasskey(val id: String) : SecurityChange
    data class SetPassword(val newPassword: String) : SecurityChange
    data object RemovePassword : SecurityChange
}

@KoinViewModel
class SecurityViewModel(
    private val security: SecurityRemoteRepository,
    private val authBackend: AuthBackend,
    private val passkeys: Passkeys,
) : ErrorViewModel() {
    val info: StateFlow<SecurityInfo?>
        field = MutableStateFlow<SecurityInfo?>(null)

    val isLoading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /** A change waiting for the user to confirm it's them. */
    val pendingChange: StateFlow<SecurityChange?>
        field = MutableStateFlow<SecurityChange?>(null)

    /** Whether passkeys can be created and used on this device. */
    val isPasskeysSupported: Boolean get() = passkeys.isSupported

    init {
        load()
    }

    private fun perform(block: suspend () -> Unit) = viewModelScope.launch(GlobalAsyncErrorHandler.coroutineExceptionHandler) {
        try {
            isLoading.value = true
            clearError()
            block()
        } catch (e: ServerException) {
            setError(e)
        } catch (e: PasskeyException) {
            setError(e)
        } finally {
            isLoading.value = false
        }
    }

    fun load() = perform { info.value = security.getSecurityInfo() }

    fun addPasskey() = perform {
        val response = passkeys.create(security.passkeyCreationOptions()) ?: return@perform
        security.addPasskey(response, passkeys.deviceName)
        info.value = security.getSecurityInfo()
    }

    /** Asks the user to confirm it's them, before making [change]. */
    fun request(change: SecurityChange) {
        clearError()
        pendingChange.value = change
    }

    fun cancelChange() {
        pendingChange.value = null
    }

    /** Confirms it's the user with one of their passkeys, and makes the pending change. */
    fun confirmWithPasskey() = perform {
        val change = pendingChange.value ?: return@perform
        val credential = passkeys.signIn(authBackend.passkeySignInOptions(), includePasswords = false)
            as? SavedCredential.Passkey ?: return@perform
        apply(change, Reauthentication(authenticationResponseJson = credential.authenticationResponseJson))
    }

    /** Confirms it's the user with their [password], and makes the pending change. */
    fun confirmWithPassword(password: String) = perform {
        val change = pendingChange.value ?: return@perform
        apply(change, Reauthentication(password = password))
    }

    private suspend fun apply(change: SecurityChange, reauthentication: Reauthentication) {
        when (change) {
            is SecurityChange.RemovePasskey -> security.removePasskey(change.id, reauthentication)
            is SecurityChange.SetPassword -> security.setPassword(change.newPassword, reauthentication)
            SecurityChange.RemovePassword -> security.removePassword(reauthentication)
        }
        pendingChange.value = null
        info.value = security.getSecurityInfo()
    }
}
