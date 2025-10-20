package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository

class LendingsManagementViewModel : ViewModel() {

    val inventoryItemTypes = InventoryItemTypesRepository.selectAllAsFlow().stateInViewModel()

    val lendings = LendingsRepository.selectAllAsFlow().stateInViewModel()

    val users = UsersRepository.selectAllAsFlow().stateInViewModel()

    fun confirm(lending: Lending) {
        viewModelScope.launch(defaultAsyncDispatcher) {
            Napier.i { "Confirming lending..." }
            LendingsRemoteRepository.confirm(lending.id)
            Napier.i { "Lending has been confirmed." }
        }
    }

    fun pickup(lending: Lending) {
        viewModelScope.launch(defaultAsyncDispatcher) {
            Napier.i { "Marking lending as picked up..." }
            LendingsRemoteRepository.pickup(lending.id)
            Napier.i { "Lending has been marked as picked up." }
        }
    }

    fun `return`(lending: Lending) {
        viewModelScope.launch(defaultAsyncDispatcher) {
            Napier.i { "Returning lending..." }
            LendingsRemoteRepository.`return`(lending.id)
            Napier.i { "Return of lending has been received." }
        }
    }
}
