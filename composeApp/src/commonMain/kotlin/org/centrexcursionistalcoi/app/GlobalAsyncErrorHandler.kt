package org.centrexcursionistalcoi.app

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalAsyncErrorHandler {
    private val _error = MutableStateFlow<Throwable?>(null)
    val error get() = _error.asStateFlow()

    fun setError(throwable: Throwable) {
        Napier.e("Unhandled exception", throwable)
        _error.value = throwable
    }

    fun clearError() {
        _error.value = null
    }
}
