package org.centrexcursionistalcoi.app.viewmodel.spaces

import androidx.lifecycle.ViewModel
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import org.centrexcursionistalcoi.app.data.Category
import org.centrexcursionistalcoi.app.data.PaymentStatus
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class SpaceLendingDetailsViewModel(
    @InjectedParam private val lendingId: Uuid,
    spaceLendingsRepository: SpaceLendingsRepository,
    spacesRepository: SpacesRepository,
    private val spaceKeysRepository: SpaceKeysRepository,
    profileRepository: ProfileRepository,
    private val remote: SpaceLendingsRemoteRepository,
) : ViewModel() {
    val lending = spaceLendingsRepository.getAsFlow(lendingId).stateInViewModel()
    val profile = profileRepository.profile.stateInViewModel()

    val space = lending.flatMapLatest { lending ->
        if (lending == null) flowOf(null) else spacesRepository.getAsFlow(lending.space)
    }.stateInViewModel()

    val keys = lending.flatMapLatest { lending ->
        if (lending == null) flowOf(emptyList()) else spaceKeysRepository.getBySpaceAsFlow(lending.space)
    }.stateInViewModel()

    private val _isWorking = MutableStateFlow(false)
    val isWorking = _isWorking.asStateFlow()

    private fun work(block: suspend () -> Unit) = launch {
        _isWorking.value = true
        try {
            block()
        } finally {
            _isWorking.value = false
        }
    }

    fun cancel() = work { remote.cancel(lendingId) }

    fun updateAttendees(attendees: Map<Category, Int>) = work { remote.updateAttendees(lendingId, attendees) }

    fun pickup() = work { remote.pickup(lendingId) }

    fun returnKeys() = work { remote.returnKeys(lendingId) }

    fun setPayment(status: PaymentStatus) = work { remote.setPayment(lendingId, status) }

    fun submitReport(notes: String?, issues: String?, notesFiles: List<PlatformFile>, issuesFiles: List<PlatformFile>, onDone: () -> Unit) = work {
        remote.submitReport(lendingId, notes, issues, notesFiles, issuesFiles)
        onDone()
    }

    fun attachPaymentProof(files: List<PlatformFile>, onDone: () -> Unit) = work {
        remote.attachPaymentProof(lendingId, files)
        onDone()
    }

    fun refresh() = work { remote.update(lendingId, ignoreIfModifiedSince = true) }
}
