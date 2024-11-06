package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel

actual fun ViewModel.launch(block: suspend () -> Unit) {
}

actual suspend fun <Result> ViewModel.uiThread(block: suspend () -> Result): Result {
    TODO("Not yet implemented")
}