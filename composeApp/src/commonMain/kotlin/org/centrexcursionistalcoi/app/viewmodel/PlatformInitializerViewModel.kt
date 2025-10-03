package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.platform.PlatformLoadLogic

class PlatformInitializerViewModel: ViewModel() {
    private val _isReady = MutableStateFlow(false)
    val isReady get() = _isReady.asStateFlow()

    init {
        viewModelScope.launch(defaultAsyncDispatcher) {
            PlatformLoadLogic.load()
            _isReady.emit(true)
        }
    }
}
