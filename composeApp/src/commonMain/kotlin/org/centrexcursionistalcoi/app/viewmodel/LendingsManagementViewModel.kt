package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class LendingsManagementViewModel : ViewModel() {

    val lendings = LendingsRepository.selectAllAsFlow().stateInViewModel()

    fun confirm(lending: ReferencedLending) {
        viewModelScope.launch(defaultAsyncDispatcher) {
            Napier.i { "Confirming lending..." }
            LendingsRemoteRepository.confirm(lending.id)
            Napier.i { "Lending has been confirmed." }
        }
    }

    fun `return`(lending: ReferencedLending) {
        viewModelScope.launch(defaultAsyncDispatcher) {
            Napier.i { "Returning lending..." }
            LendingsRemoteRepository.`return`(lending.id)
            Napier.i { "Return of lending has been received." }
        }
    }

    fun delete(lending: ReferencedLending) = launch {
        Napier.i { "Deleting lending..." }
        LendingsRemoteRepository.delete(lending.id)
        Napier.i { "Lending has been deleted." }
    }

    fun skipMemory(lending: ReferencedLending) = launch {
        Napier.i { "Skipping memory for lending..." }
        LendingsRemoteRepository.skipMemory(lending.id)
        Napier.i { "Memory for lending has been skipped." }
    }
}
