package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository

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
}
