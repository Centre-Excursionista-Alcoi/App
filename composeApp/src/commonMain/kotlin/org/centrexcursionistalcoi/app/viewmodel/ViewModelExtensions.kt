package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel

expect fun ViewModel.launch(block: suspend () -> Unit)
