package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

abstract class ViewModelBase: ViewModel() {
    fun <T> Flow<T>.stateInViewModel(
        started: SharingStarted = SharingStarted.WhileSubscribed(5_000),
        initialValue: T? = null,
    ) = stateIn(viewModelScope, started, initialValue)
}
