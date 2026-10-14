package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

abstract class ErrorViewModel : ViewModel() {
    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()

    /** [error], in terms of what the user is told. */
    val authError: StateFlow<AuthError?> = error.map { it?.toAuthError() }.stateInViewModel()

    @Deprecated("Use setError with Throwable parameter")
    protected fun setError(message: String?) {
        _error.value = Throwable(message)
    }

    protected fun setError(exception: Throwable) {
        _error.value = exception
    }

    fun clearError() {
        _error.value = null
    }
}
