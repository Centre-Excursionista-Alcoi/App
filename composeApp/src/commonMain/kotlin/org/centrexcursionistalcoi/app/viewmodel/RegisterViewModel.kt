package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import ceaapp.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.error.AuthException
import org.centrexcursionistalcoi.app.error.ServerException
import org.centrexcursionistalcoi.app.network.Auth
import org.centrexcursionistalcoi.app.route.Login
import org.jetbrains.compose.resources.getString

class RegisterViewModel : ViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun register(
        navController: NavController,
        email: String,
        password: String,
        firstName: String,
        familyName: String,
        nif: String,
        phone: String
    ) = launch {
        try {
            _isLoading.emit(true)
            Napier.i { "Registering $email..." }
            Auth.register(email, password, firstName, familyName, nif, phone)
            Napier.i { "Registered successfully." }
            navController.navigate(Login)
        } catch (e: ServerException) {
            when (e) {
                is AuthException.WrongCredentials -> {
                    Napier.e { "Wrong credentials" }
                    _error.emit(getString(Res.string.error_wrong_credentials))
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
