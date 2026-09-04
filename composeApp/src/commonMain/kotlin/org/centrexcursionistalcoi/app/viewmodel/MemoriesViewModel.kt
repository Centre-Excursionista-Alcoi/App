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
     * The memories submitted **by the current user**. Always editable by them.
     */
    val memories = combine(
        profile,
        memoriesRepository.selectAllAsFlow(),
    ) { profile, memories ->
        val profileValue = profile ?: return@combine null
        memories.filter { it.submittedBy == profileValue }
    }.stateInViewModel()

    /**
     * Memories the current user is tagged as a participating member on, but did **not** submit themselves.
     * Read-only: only the submitter (or an admin) can modify a memory.
     */
    val taggedMemories = combine(
        profile,
        memoriesRepository.selectAllAsFlow(),
    ) { profile, memories ->
        val profileValue = profile ?: return@combine null
        memories.filter { memory ->
            memory.submittedBy != profileValue && memory.members.any { it.memberNumber == profileValue.memberNumber }
        }
    }.stateInViewModel()
}
