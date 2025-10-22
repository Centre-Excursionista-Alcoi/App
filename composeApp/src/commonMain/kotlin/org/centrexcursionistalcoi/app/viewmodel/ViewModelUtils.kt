package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

context(vm: ViewModel)
fun <T> Flow<T>.stateInViewModel(
    started: SharingStarted = SharingStarted.WhileSubscribed(5_000),
    initialValue: T? = null,
) = stateIn(vm.viewModelScope, started, initialValue)

/**
 * Launches a new coroutine in the UI thread.
 */
fun ViewModel.launch(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = viewModelScope.launch(Dispatchers.Main, start, block)
