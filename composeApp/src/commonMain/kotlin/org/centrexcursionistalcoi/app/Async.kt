package org.centrexcursionistalcoi.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

expect val defaultAsyncDispatcher : CoroutineDispatcher

/**
 * Switches to the async coroutine context ([defaultAsyncDispatcher]).
 */
context(_: CoroutineScope)
suspend fun <T> doAsync(block: suspend CoroutineScope.() -> T) = withContext(defaultAsyncDispatcher) { block() }

/**
 * Switches to the main coroutine context ([Dispatchers.Main]).
 */
suspend fun <T> doMain(block: suspend CoroutineScope.() -> T) = withContext(Dispatchers.Main) { block() }
