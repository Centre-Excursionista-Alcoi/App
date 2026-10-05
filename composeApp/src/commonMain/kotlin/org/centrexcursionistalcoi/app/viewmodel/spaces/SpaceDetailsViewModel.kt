package org.centrexcursionistalcoi.app.viewmodel.spaces

import androidx.lifecycle.ViewModel
import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.data.SpaceOccupancy
import org.centrexcursionistalcoi.app.database.SpaceKeyTypesRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.Uuid

@KoinViewModel
class SpaceDetailsViewModel(
    @InjectedParam private val spaceId: Uuid,
    spacesRepository: SpacesRepository,
    spaceKeyTypesRepository: SpaceKeyTypesRepository,
    private val spaceLendingsRemoteRepository: SpaceLendingsRemoteRepository,
) : ViewModel() {
    private val log = logging()

    val space = spacesRepository.getAsFlow(spaceId).stateInViewModel()
    val keys = spaceKeyTypesRepository.selectAllAsFlow().forSpace(spaceId).stateInViewModel()

    private val _occupancy = MutableStateFlow<List<SpaceOccupancy>?>(null)
    /** The nights that are taken, or `null` while loading. */
    val occupancy = _occupancy.asStateFlow()

    init {
        refreshOccupancy()
    }

    fun refreshOccupancy() = launch {
        try {
            _occupancy.value = spaceLendingsRemoteRepository.occupancy(spaceId).sortedBy { it.checkIn }
        } catch (e: Exception) {
            log.w(e) { "Could not load the occupancy of space $spaceId" }
            _occupancy.value = emptyList()
        }
    }
}
