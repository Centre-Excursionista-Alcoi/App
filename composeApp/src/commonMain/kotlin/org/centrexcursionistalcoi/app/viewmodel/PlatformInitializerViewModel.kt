package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic
import org.centrexcursionistalcoi.app.push.SSENotificationsListener

class PlatformInitializerViewModel(url: Url?): ViewModel() {
    private val _isReady = MutableStateFlow(false)
    val isReady get() = _isReady.asStateFlow()

    private val _startDestination = MutableStateFlow<Destination?>(null)
    val startDestination get() = _startDestination.asStateFlow()

    init {
        viewModelScope.launch(defaultAsyncDispatcher) {
            PlatformLoadLogic.load()

            if (url != null) {
                Napier.d { "Processing destination for url: $url" }
                _startDestination.value = Destination.fromUrl(url)
            }

            SSENotificationsListener.startListening()

            _isReady.emit(true)
        }
    }
}
