package org.centrexcursionistalcoi.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val defaultAsyncDispatcher: CoroutineDispatcher = Dispatchers.Main
