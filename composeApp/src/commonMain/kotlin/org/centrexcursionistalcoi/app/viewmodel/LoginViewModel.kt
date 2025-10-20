package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.AuthFlowLogic
import org.centrexcursionistalcoi.app.network.getHttpClient

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

    fun register(username: String, name: String, email: String, password: String) = viewModelScope.async {
        try {
            _isRegistering.emit(true)
            _registerError.emit(null)

            val response = getHttpClient().submitForm(
                url = "/register",
                formParameters = parameters {
                    append("username", username)
                    append("name", name)
                    append("email", email)
                    append("password", password)
                }
            )
            if (response.status.isSuccess()) {
                Napier.d { "Registration successful." }
                true
            } else {
                val body = response.bodyAsText()
                Napier.d { "Registration failed (${response.status}): $body" }
                _registerError.value = "Registration failed (${response.status}): $body"
                false
            }
        } finally {
            _isRegistering.emit(false)
        }
    }
}
