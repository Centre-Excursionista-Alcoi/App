package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Sports
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.ProfileRemoteRepository

class HomeViewModel: ViewModel() {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    val profile = ProfileRepository.profile.stateInViewModel()

    val departments = DepartmentsRepository.selectAllAsFlow().stateInViewModel()

    val users = UsersRepository.selectAllAsFlow().stateInViewModel()

    val posts = PostsRepository.selectAllAsFlow().stateInViewModel()

    init {
        viewModelScope.launch {
            val departments = DepartmentsRepository.selectAll()
            Napier.i { "Departments: $departments" }
        }
    }

    fun createDepartment(displayName: String, imageFile: PlatformFile?) = viewModelScope.launch(defaultAsyncDispatcher) {
        val image = imageFile?.readBytes()
        DepartmentsRemoteRepository.create(displayName, image)
    }

    fun delete(department: Department) = viewModelScope.launch(defaultAsyncDispatcher) {
        DepartmentsRemoteRepository.delete(department.id)
    }

    fun signUpForLending(fullName: String, nif: String, phoneNumber: String, sports: List<Sports>, address: String, postalCode: String, city: String, province: String, country: String) = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.signUpForLending(fullName, nif, phoneNumber, sports, address, postalCode, city, province, country)
        ProfileRemoteRepository.synchronize()
    }

    fun createInsurance(company: String, policyNumber: String, validFrom: LocalDate, validTo: LocalDate) = viewModelScope.launch(defaultAsyncDispatcher) {
        ProfileRemoteRepository.createInsurance(company, policyNumber, validFrom, validTo)
        ProfileRemoteRepository.synchronize()
    }

    fun sync() = viewModelScope.launch(defaultAsyncDispatcher) {
        try {
            _isSyncing.emit(true)
            LoadingViewModel.syncAll(force = true)
        } finally {
            _isSyncing.emit(false)
        }
    }
}
