package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import ceaapp.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.centrexcursionistalcoi.app.auth.Account
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.error.AuthException
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.push.PushNotifications
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.validation.isValidEmail
import org.jetbrains.compose.resources.getString

class LoginViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _credentials = MutableStateFlow(EmailAndPassword())
    val credentials get() = _credentials.asStateFlow()

    val valid = credentials
        .map { (email, password) -> email.isNotBlank() && email.isValidEmail && password.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    data class EmailAndPassword(val email: String = "", val password: String = "")

    fun setEmail(email: String) {
        _credentials.tryEmit(_credentials.value.copy(email = email))
        _error.tryEmit(null)
    }

    fun setPassword(password: String) {
        _credentials.tryEmit(_credentials.value.copy(password = password))
        _error.tryEmit(null)
    }

    fun login(navController: NavController) = launch {
        if (!valid.value || isLoading.value) return@launch

        try {
            _isLoading.emit(true)
            val (email, password) = credentials.value
            Napier.i { "Logging is as $email..." }
            AuthBackend.login(email, password)
            Napier.i { "Logged in successfully." }
            AccountManager.put(Account(email), password)
            PushNotifications.refreshTokenOnServer()
            uiThread {
                navController.navigate(Loading)
            }
        } catch (e: ServerException) {
            when (e) {
                is AuthException.WrongCredentials -> {
                    // TODO: Handle wrong credentials
                    Napier.e { "Wrong credentials" }
                    _error.emit(getString(Res.string.error_wrong_credentials))
                }
                is AuthException.UserNotConfirmed -> {
                    Napier.e { "User not confirmed" }
                    _error.emit(getString(Res.string.error_user_not_confirmed))
                }
                else -> {
                    // TODO: Handle other errors
                    Napier.e(e) { "Could not log in" }
                    _error.emit(getString(Res.string.error_unknown, e.code ?: 0, e.response))
                }
            }
        } finally {
            _isLoading.emit(false)
        }
    }
}
