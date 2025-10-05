package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository

class HomeViewModel: ViewModel() {
    val profile = ProfileRepository.profile.stateInViewModel()

    val departments = DepartmentsRepository.selectAllAsFlow().stateInViewModel()

    val posts = PostsRepository.selectAllAsFlow().stateInViewModel()

    init {
        viewModelScope.launch {
            val departments = DepartmentsRepository.selectAll()
            Napier.i { "Departments: $departments" }
        }
    }

    fun createDepartment(displayName: String) = viewModelScope.launch(defaultAsyncDispatcher) {
        DepartmentsRemoteRepository.create(displayName)
    }

    fun delete(department: Department) = viewModelScope.launch(defaultAsyncDispatcher) {
        DepartmentsRemoteRepository.delete(department.id)
    }

    fun signUpForLending(fullName: String, nif: String, phoneNumber: String, sports: List<Sports>, address: String, postalCode: String, city: String, province: String, country: String) = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.signUpForLending(fullName, nif, phoneNumber, sports, address, postalCode, city, province, country)
        ProfileRemoteRepository.synchronize()
    }
}
