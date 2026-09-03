package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class LendingsManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    lendingsRepository: LendingsRepository,
    usersRepository: UsersRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository
) : ViewModel() {
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()
    val users = usersRepository.selectAllAsFlow().stateInViewModel()

    fun confirmLending(lending: ReferencedLending) = launch {
        withContext(dispatcherProvider.io) {
            lendingsRemoteRepository.confirm(lending.id)
        }
    }

    fun skipLendingMemory(lending: ReferencedLending) = launch {
        withContext(dispatcherProvider.io) {
            lendingsRemoteRepository.skipMemory(lending.id)
        }
    }

    fun delete(lending: ReferencedLending, reason: String?) = launch {
        withContext(dispatcherProvider.io) {
            lendingsRemoteRepository.delete(lending.id, reason)
        }
    }
}
