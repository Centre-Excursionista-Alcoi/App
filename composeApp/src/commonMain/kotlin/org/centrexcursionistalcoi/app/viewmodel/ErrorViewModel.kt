package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class ErrorViewModel : ViewModel() {
    private val _error = MutableStateFlow<Throwable?>(null)
    val error = _error.asStateFlow()

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
