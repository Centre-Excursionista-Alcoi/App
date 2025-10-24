package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.auth.AuthFlowLogic

class LoginViewModel: ViewModel() {
    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn get() = _isLoggingIn.asStateFlow()

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering get() = _isRegistering.asStateFlow()

    private val _registerError = MutableStateFlow<String?>(null)
    val registerError get() = _registerError.asStateFlow()

    fun login() = viewModelScope.launch {
        try {
            if (isLoggingIn.value) {
                Napier.d { "Already loading." }
                return@launch
            }

            _isLoggingIn.emit(true)

            AuthFlowLogic.start()

            _isLoggingIn.emit(false)
        } finally {
            _isLoggingIn.emit(false)
        }
    }

    fun clearErrors() {
        _registerError.value = null
    }

    fun register(username: String, name: String, email: String, password: String): Deferred<Boolean> = viewModelScope.async {
        try {
            _isRegistering.emit(true)
            _registerError.emit(null)

            val exception = AuthBackend.register(username, name, email, password)
            if (exception != null) {
                _registerError.emit(exception.message ?: "Unknown error")
                false
            } else {
                true
            }
        } finally {
            _isRegistering.emit(false)
        }
    }
}
