package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.AuthBackend
import org.centrexcursionistalcoi.app.doMain
import org.centrexcursionistalcoi.app.exception.ServerException

class LoginViewModel : ErrorViewModel() {
    private val _isLoading = MutableStateFlow(false)
    val isLoading get() = _isLoading.asStateFlow()

    fun login(nif: String, password: String, afterLogin: () -> Unit) = viewModelScope.launch {
        try {
            _isLoading.emit(true)

            AuthBackend.login(nif, password)

            doMain { afterLogin() }
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }

    fun register(nif: String, password: String, afterLogin: () -> Unit) = viewModelScope.async {
        try {
            _isLoading.emit(true)
            clearError()

            // Try to register
            AuthBackend.register(nif, password)

            // If successful, log in
            AuthBackend.login(nif, password)

            doMain { afterLogin() }
        } catch (e: ServerException) {
            setError(e)
        } finally {
            _isLoading.emit(false)
        }
    }
}
