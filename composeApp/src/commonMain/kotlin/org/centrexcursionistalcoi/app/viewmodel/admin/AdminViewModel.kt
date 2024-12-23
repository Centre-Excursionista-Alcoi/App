package org.centrexcursionistalcoi.app.viewmodel.admin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.database.entity.DatabaseEntity
import org.centrexcursionistalcoi.app.network.Sync
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.uiThread

abstract class AdminViewModel: ViewModel() {
    protected fun <SerializableType : DatabaseData, LocalType : DatabaseEntity<SerializableType>> ViewModel.onCreateOrUpdate(
        value: LocalType,
        creating: MutableStateFlow<Boolean>,
        creator: suspend (LocalType) -> Unit,
        updater: suspend (LocalType) -> Unit,
        onCreate: () -> Unit
    ) {
        launch {
            try {
                creating.emit(true)
                if (value.id <= 0) {
                    creator(value)
                } else {
                    updater(value)
                }

                Sync.syncBasics()
                Sync.syncBookings()

                uiThread { onCreate() }
            } finally {
                creating.emit(false)
            }
        }
    }
}
