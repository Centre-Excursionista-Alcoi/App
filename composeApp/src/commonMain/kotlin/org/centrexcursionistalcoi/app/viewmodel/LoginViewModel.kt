package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.auth.CredentialsStore
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LoginViewModel(
    private val authBackend: AuthBackend,
    private val dispatcherProvider: DispatcherProvider,
    credentialsStore: CredentialsStore,
) : ErrorViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading get() = _isLoading.asStateFlow()

    // Reaching the Login screen with an account already saved shouldn't normally happen (both a normal logout
    // and an exhausted auto-relogin already clear it, see AuthBackend), but the system "Add account" flow
    // (AccountAuthenticator.addAccount, Android only) can land here regardless -- surface it so the user can
    // choose to forget the old one instead of silently ending up with it replaced on a fresh login.
    //
    // A one-time snapshot, not a live mirror of CredentialsStore.current: this warns about an account that was
    // *already* saved when this screen was reached, not one saved as a side effect of using this same screen.
    // login()/register() below call authBackend.login(), which saves the new credentials as soon as the server
    // accepts them, well before afterLogin() navigates away -- if this stayed live, that save flipped
    // existingAccountEmail non-null while still on this screen, popping the "you already have an account saved"
    // dialog for the account the user had just finished logging into. LoginScreen's own
    // dismissedExistingAccountWarning flag (not this flow) already handles dismissing the dialog when the user
    // taps either button, so losing liveness here costs nothing.
    val existingAccountEmail: StateFlow<String?> = MutableStateFlow(credentialsStore.current.value?.email).asStateFlow()

    fun forgetExistingAccount() = viewModelScope.launch(dispatcherProvider.io) {
        authBackend.forgetLocalAccount()
    }

    fun login(email: String, password: String, afterLogin: () -> Unit) = viewModelScope.launch {
        try {
            _isLoading.emit(true)

            authBackend.login(email, password)
            ProfileRemoteRepository.synchronize(ignoreIfModifiedSince = true)

            withContext(dispatcherProvider.main) { afterLogin() }
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    fun register(email: String, password: String, afterLogin: () -> Unit) = viewModelScope.async {
        try {
            _isLoading.emit(true)
            clearError()

            // Try to register
            authBackend.register(email, password)

            // If successful, log in
            login(email, password, afterLogin).join()
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    fun forgotPassword(email: String, afterRequest: () -> Unit) = viewModelScope.launch {
        try {
            _isLoading.emit(true)
            clearError()

            authBackend.forgotPassword(email)

            withContext(dispatcherProvider.main) { afterRequest() }
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }
}
