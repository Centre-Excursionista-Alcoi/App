package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.getHttpClient

class HomeViewModel: ViewModel() {
    private val httpClient = getHttpClient()

    val departments = DepartmentsRepository.selectAllAsFlow()
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun loadDepartments() = viewModelScope.launch {
        withContext(defaultAsyncDispatcher) {
            DepartmentsRemoteRepository.synchronizeWithDatabase()
        }
    }

    fun createDepartment(name: String) = viewModelScope.launch {
        withContext(defaultAsyncDispatcher) {
            try {
                DepartmentsRemoteRepository.create(
                    Department(0L, name)
                )

                loadDepartments()
            } catch (e: Exception) {
                Napier.e("Error creating department", e)
            }
        }
    }
}
