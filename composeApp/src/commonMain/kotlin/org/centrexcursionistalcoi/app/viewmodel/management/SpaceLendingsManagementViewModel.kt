package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel

/**
 * For the people who manage space lendings: all of them.
 */
@KoinViewModel
class SpaceLendingsManagementViewModel(
    spaceLendingsRepository: SpaceLendingsRepository,
    spacesRepository: SpacesRepository,
) : ViewModel() {
    val lendings = spaceLendingsRepository.selectAllAsFlow().stateInViewModel()
    val spaces = spacesRepository.selectAllAsFlow().stateInViewModel()
}
