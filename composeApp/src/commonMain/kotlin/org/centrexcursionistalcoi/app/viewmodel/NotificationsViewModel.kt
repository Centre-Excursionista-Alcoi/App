package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.centrexcursionistalcoi.app.database.appDatabase

class NotificationsViewModel : ViewModel() {
    private val notificationsDao = appDatabase.notificationsDao()

    val notifications get() = notificationsDao
        .getAllNotificationsAsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
