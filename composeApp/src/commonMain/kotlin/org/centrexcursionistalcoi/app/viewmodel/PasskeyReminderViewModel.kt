package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.PasskeyException
import org.centrexcursionistalcoi.app.auth.PasskeyUpgrade
import org.centrexcursionistalcoi.app.auth.Passkeys
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.SecurityRemoteRepository
import org.koin.core.annotation.KoinViewModel

/** Reminds users who still sign in with a password to create a passkey, see [PasskeyUpgrade]. */
@KoinViewModel
class PasskeyReminderViewModel(
    private val passkeyUpgrade: PasskeyUpgrade,
    private val passkeys: Passkeys,
    private val security: SecurityRemoteRepository,
) : ErrorViewModel() {
    val isShowing: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        viewModelScope.launch { isShowing.value = passkeyUpgrade.shouldRemind() }
    }

    fun createPasskey() = viewModelScope.launch {
        try {
            isLoading.value = true
            clearError()
            val response = passkeys.create(security.passkeyCreationOptions()) ?: return@launch
            security.addPasskey(response, passkeys.deviceName)
            isShowing.value = false
        } catch (e: ServerException) {
            setError(e)
        } catch (e: PasskeyException) {
            setError(e)
        } finally {
            isLoading.value = false
        }
    }

    fun notNow() = viewModelScope.launch {
        isShowing.value = false
        passkeyUpgrade.snooze()
    }
}
