package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class LoadingViewModel : ViewModel() {
    fun load(
        onLoggedIn: (name: String, groups: List<String>) -> Unit,
        onNotLoggedIn: () -> Unit,
    ) = viewModelScope.launch {
        return@launch onNotLoggedIn() // Temporarily disable login check
    }
}
