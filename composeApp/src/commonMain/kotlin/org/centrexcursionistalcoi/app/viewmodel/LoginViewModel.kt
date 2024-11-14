package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import ceaapp.composeapp.generated.resources.Res
import ceaapp.composeapp.generated.resources.error_unknown
import ceaapp.composeapp.generated.resources.error_user_not_confirmed
import ceaapp.composeapp.generated.resources.error_wrong_credentials
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.auth.Account
import org.centrexcursionistalcoi.app.auth.AccountManager
import org.centrexcursionistalcoi.app.error.AuthException
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.network.AuthBackend
import org.centrexcursionistalcoi.app.route.Loading
import org.jetbrains.compose.resources.getString

class LoginViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(navController: NavController, email: String, password: String) = launch {
        try {
            _isLoading.emit(true)
            Napier.i { "Logging is as $email..." }
            AuthBackend.login(email, password)
            Napier.i { "Logged in successfully." }
            AccountManager.put(Account(email), password)
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

    fun clearError() {
        _error.tryEmit(null)
    }
}
