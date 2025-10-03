package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository

class HomeViewModel: ViewModel() {
    val profile = ProfileRepository.profile.stateInViewModel()

    val departments by lazy { DepartmentsRepository.selectAllAsFlow().stateInViewModel() }

    val posts by lazy { PostsRepository.selectAllAsFlow().stateInViewModel() }
}
