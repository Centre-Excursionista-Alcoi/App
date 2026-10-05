package org.centrexcursionistalcoi.app.viewmodel.spaces

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel

/**
 * The spaces anyone can book, and the user's own lendings of them.
 */
@KoinViewModel
class SpacesViewModel(
    spacesRepository: SpacesRepository,
    spaceLendingsRepository: SpaceLendingsRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {
    val spaces = spacesRepository.selectAllAsFlow().stateInViewModel()

    val myLendings = combine(spaceLendingsRepository.selectAllAsFlow(), profileRepository.profile) { lendings, profile ->
        lendings.filter { it.userSub != null && it.userSub == profile?.sub }.sortedByDescending { it.checkIn }
    }.stateInViewModel()
}
