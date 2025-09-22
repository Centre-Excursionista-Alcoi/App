package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.AuthFlowLogic
import org.centrexcursionistalcoi.app.network.getHttpClient

class LoginViewModel: ViewModel() {
    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn get() = _isLoggingIn.asStateFlow()

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
}
