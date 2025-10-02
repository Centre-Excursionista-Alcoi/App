package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.auth.AuthCallbackProcessor

class AuthCallbackModel : ViewModel() {
    fun processCallbackUrl(url: Url) = viewModelScope.launch(Dispatchers.IO) {
        Napier.i { "Processing callback URL ($url)..." }
        AuthCallbackProcessor.processCallbackUrl(url)
    }
}
