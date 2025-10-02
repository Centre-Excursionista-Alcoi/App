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
import org.centrexcursionistalcoi.app.auth.AuthCallbackProcessor
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher

class AuthCallbackModel : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun processCallbackUrl(url: Url, afterLogin: () -> Unit) =
        viewModelScope.launch(defaultAsyncDispatcher) {
            try {
                Napier.i { "Processing callback URL ($url)..." }
                AuthCallbackProcessor.processCallbackUrl(url)
                withContext(Dispatchers.Main) { afterLogin() }
            } catch (e: Exception) {
                Napier.e("Error processing callback URL", e)
                _error.value = e.message
            }
        }
}
