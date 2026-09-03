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
    private val profile = ProfileRepository.profile.stateInViewModel()

    /**
     * The memories submitted **by the current user**.
     */
    val memories = combine(
        profile,
        memoriesRepository.selectAllAsFlow(),
    ) { profile, memories ->
        val profileValue = profile ?: return@combine null
        memories.filter { it.submittedBy == profileValue }
    }.stateInViewModel()
}
