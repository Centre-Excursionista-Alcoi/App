package org.centrexcursionistalcoi.app

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val defaultAsyncDispatcher: CoroutineDispatcher = Dispatchers.IO
