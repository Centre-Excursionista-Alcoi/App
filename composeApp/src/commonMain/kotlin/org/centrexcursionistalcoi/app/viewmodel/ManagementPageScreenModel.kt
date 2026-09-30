package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ManagementPageScreenModel(
    profileRepository: ProfileRepository,
    departmentsRepository: DepartmentsRepository,
    lendingsRepository: LendingsRepository,
) : ViewModel() {
    val profile = profileRepository.profile.stateInViewModel()
    val departments = departmentsRepository.selectAllAsFlow().stateInViewModel()
    val lendings = lendingsRepository.selectAllAsFlow().stateInViewModel()
}
