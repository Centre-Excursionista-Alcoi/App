package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.GlobalAsyncErrorHandler
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.auth.CredentialsStore
import org.centrexcursionistalcoi.app.auth.PasskeyException
import org.centrexcursionistalcoi.app.auth.PasskeyUpgrade
import org.centrexcursionistalcoi.app.auth.Passkeys
import org.centrexcursionistalcoi.app.auth.SavedCredential
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LoginViewModel(
    private val authBackend: AuthBackend,
    private val dispatcherProvider: DispatcherProvider,
    credentialsStore: CredentialsStore,
    private val profileRemoteRepository: ProfileRemoteRepository,
    private val passkeys: Passkeys,
    private val passkeyUpgrade: PasskeyUpgrade,
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

    fun forgetExistingAccount() = viewModelScope.launch(dispatcherProvider.io + GlobalAsyncErrorHandler.coroutineExceptionHandler) {
        authBackend.forgetLocalAccount()
    }

    /** Whether the user can sign in with a passkey, or a password saved in the platform's password manager. */
    val canUseSavedCredentials: Boolean get() = passkeys.isSupported

    /**
     * Logs in with a password the user typed.
     * @param isSaved Whether the password came from the password manager, so it doesn't need saving.
     */
    fun login(email: String, password: String, isSaved: Boolean = false, afterLogin: () -> Unit) = viewModelScope.launch(GlobalAsyncErrorHandler.coroutineExceptionHandler) {
        try {
            _isLoading.emit(true)
            clearError()

            authBackend.login(email, password)
            if (!isSaved) passkeys.savePassword(email, password)
            profileRemoteRepository.synchronize(ignoreIfModifiedSince = true)
            // The platform may upgrade the account to a passkey now, right after signing in with a password.
            passkeyUpgrade.afterPasswordLogin()

            withContext(dispatcherProvider.main) { afterLogin() }
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    /**
     * Shows the platform's sign-in sheet, with the user's passkeys and saved passwords, and logs in with the one they
     * pick. If they have nothing saved, or cancel, nothing happens: the email and password form is still there.
     */
    fun signInWithSavedCredential(afterLogin: () -> Unit) = viewModelScope.launch(GlobalAsyncErrorHandler.coroutineExceptionHandler) {
        try {
            _isLoading.emit(true)
            clearError()

            val options = authBackend.passkeySignInOptions()
            when (val credential = passkeys.signIn(options)) {
                null -> return@launch
                is SavedCredential.Password -> {
                    login(credential.email, credential.password, isSaved = true, afterLogin = afterLogin).join()
                    return@launch
                }
                is SavedCredential.Passkey -> authBackend.loginWithPasskey(credential.authenticationResponseJson)
            }
            profileRemoteRepository.synchronize(ignoreIfModifiedSince = true)

            withContext(dispatcherProvider.main) { afterLogin() }
        } catch (e: ServerException) {
            setError(e)
        } catch (e: PasskeyException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    /** Where the user is in registering, see [RegistrationStep]. */
    val registrationStep: StateFlow<RegistrationStep>
        field = MutableStateFlow<RegistrationStep>(RegistrationStep.Email)

    /** Whether registering can create a passkey on this device, instead of a password. */
    val canRegisterWithPasskey: Boolean get() = passkeys.isSupported

    /** Emails [email] the code that proves it's theirs, and asks for it. */
    fun requestRegistrationCode(email: String) = viewModelScope.launch(GlobalAsyncErrorHandler.coroutineExceptionHandler) {
        try {
            _isLoading.emit(true)
            clearError()

            authBackend.requestRegistrationCode(email.trim())
            registrationStep.value = RegistrationStep.Code(email.trim())
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    /** The user entered the emailed [code]: it's checked when the account is created. */
    fun enterRegistrationCode(code: String) {
        val step = registrationStep.value as? RegistrationStep.Code ?: return
        clearError()
        registrationStep.value = RegistrationStep.Method(step.email, code.trim())
    }

    /** Goes back a step, e.g. to fix the email, or a wrong code. */
    fun registrationBack() {
        clearError()
        registrationStep.value = when (val step = registrationStep.value) {
            RegistrationStep.Email, is RegistrationStep.Code -> RegistrationStep.Email
            is RegistrationStep.Method -> RegistrationStep.Code(step.email)
        }
    }

    /** Registers the account with a new passkey, and logs it in. */
    fun registerWithPasskey(afterLogin: () -> Unit) = viewModelScope.launch(GlobalAsyncErrorHandler.coroutineExceptionHandler) {
        val step = registrationStep.value as? RegistrationStep.Method ?: return@launch
        try {
            _isLoading.emit(true)
            clearError()

            val options = authBackend.passkeyRegistrationOptions(step.email, step.code)
            val response = passkeys.create(options) ?: return@launch
            authBackend.registerWithPasskey(step.email, step.code, response, passkeys.deviceName)
            profileRemoteRepository.synchronize(ignoreIfModifiedSince = true)

            withContext(dispatcherProvider.main) { afterLogin() }
        } catch (e: ServerException) {
            setError(e)
        } catch (e: PasskeyException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    /** Registers the account with [password], and logs it in. */
    fun register(password: String, afterLogin: () -> Unit) = viewModelScope.async {
        val step = registrationStep.value as? RegistrationStep.Method ?: return@async
        try {
            _isLoading.emit(true)
            clearError()

            // Try to register
            authBackend.register(step.email, password, step.code)

            // If successful, log in
            login(step.email, password, afterLogin = afterLogin).join()
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    fun forgotPassword(email: String, afterRequest: () -> Unit) = viewModelScope.launch(GlobalAsyncErrorHandler.coroutineExceptionHandler) {
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

/** The steps of registering: the email, the code emailed to it, and how the account will sign in. */
sealed interface RegistrationStep {
    data object Email : RegistrationStep
    data class Code(val email: String) : RegistrationStep
    data class Method(val email: String, val code: String) : RegistrationStep
}
