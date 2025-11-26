package org.centrexcursionistalcoi.app

import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object GlobalAsyncErrorHandler {
    private val log = logging()
    private val _error = MutableStateFlow<Throwable?>(null)
    val error get() = _error.asStateFlow()

    fun setError(throwable: Throwable) {
        log.e(throwable) { "Unhandled exception" }
        _error.value = throwable
    }

    fun clearError() {
        _error.value = null
    }
}
