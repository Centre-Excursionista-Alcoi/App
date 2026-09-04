package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import io.ktor.http.Url
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.nav.Destination
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class PlatformInitializerViewModel(
    @InjectedParam url: Url?,
    dispatcherProvider: DispatcherProvider,
    private val sseNotificationsListener: SSENotificationsListener,
) : ViewModel() {
    private val log = logging()

    private val _isReady = MutableStateFlow(false)
    val isReady get() = _isReady.asStateFlow()

    private val _startDestination = MutableStateFlow<Destination?>(null)
    val startDestination get() = _startDestination.asStateFlow()

    init {
        viewModelScope.launch(dispatcherProvider.io) {
            log.d { "Running platform loading logic..." }
            PlatformLoadLogic.load()

            if (url != null) {
                log.d { "Processing destination for url: $url" }
                _startDestination.value = Destination.fromUrl(url)
            }

            log.d { "Listening for SSE notifications..." }
            sseNotificationsListener.startListening()

            log.d { "Platform is ready." }
            _isReady.emit(true)
        }
    }
}
