package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.ReferencedMemory
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.network.MemoriesRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MemoriesManagementViewModel(
    private val dispatcherProvider: DispatcherProvider,
    memoriesRepository: MemoriesRepository,
    private val memoriesRemoteRepository: MemoriesRemoteRepository,
) : ViewModel() {
    val memories = memoriesRepository.selectAllAsFlow()
        .map { list ->
            // sort by date, showing newest first
            list.sortedByDescending { it.from }
        }
        .stateInViewModel()

    fun delete(memory: ReferencedMemory) = viewModelScope.launch(dispatcherProvider.io) {
        memoriesRemoteRepository.delete(memory.id)
    }
}
