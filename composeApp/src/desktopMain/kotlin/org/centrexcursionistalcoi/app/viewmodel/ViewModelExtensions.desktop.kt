package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

actual fun ViewModel.launch(block: suspend () -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
        block()
    }
}

actual suspend fun <Result> ViewModel.uiThread(block: suspend () -> Result): Result {
    return withContext(Dispatchers.Main) {
        block()
    }
}
