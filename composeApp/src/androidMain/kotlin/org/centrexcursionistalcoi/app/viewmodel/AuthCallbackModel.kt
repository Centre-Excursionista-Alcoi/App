package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthCallbackModel : ViewModel() {

    private val _error = MutableStateFlow<Throwable?>(null)
    val error get() = _error.asStateFlow()

    fun processCallbackUrl(url: Url, afterLogin: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            _error.value = null
            Napier.i { "Processing callback URL ($url)..." }
            AuthCallbackProcessor.processCallbackUrl(url)
            withContext(Dispatchers.Main) { afterLogin() }
        } catch (t: Throwable) {
            Napier.e(t) { "Could not process callback url." }
            _error.value = t
        }
    }
}
