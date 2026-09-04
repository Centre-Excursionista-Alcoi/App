package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.database.MemoriesRepository
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class MemoriesViewModel(
    memoriesRepository: MemoriesRepository,
) : ViewModel() {
    val profile = ProfileRepository.profile.stateInViewModel()

    /**
     * The memories relevant to the current user: the ones they submitted themselves, plus the ones they're
     * tagged as a participating member on.
     */
    val memories = combine(
        profile,
        memoriesRepository.selectAllAsFlow(),
    ) { profile, memories ->
        val profileValue = profile ?: return@combine null
        memories.filter { memory ->
            memory.submittedBy == profileValue || memory.members.any { it.memberNumber == profileValue.memberNumber }
        }
    }.stateInViewModel()
}
