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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

/**
 * Launches a new coroutine in the UI thread.
 * The coroutine will acquire the provided [lock] before executing [block], ensuring that only one coroutine
 * can execute the block at a time.
 * @param lock The [Mutex] to acquire before executing the block.
 * @param start The coroutine start option.
 * @param block The suspend function to execute within the lock.
 */
fun ViewModel.launchWithLock(
    lock: Mutex,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) = viewModelScope.launch(Dispatchers.Main, start) {
    lock.withLock { block() }
}
