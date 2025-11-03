package org.centrexcursionistalcoi.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalAsyncErrorHandler {
    private val _error = MutableStateFlow<Throwable?>(null)
    val error get() = _error.asStateFlow()

    fun setError(throwable: Throwable) {
        _error.value = throwable
    }

    fun clearError() {
        _error.value = null
    }
}
