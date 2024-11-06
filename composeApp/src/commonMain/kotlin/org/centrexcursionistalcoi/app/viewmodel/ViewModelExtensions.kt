package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel

expect fun ViewModel.launch(block: suspend () -> Unit)

expect suspend fun <Result> ViewModel.uiThread(block: suspend () -> Result): Result
