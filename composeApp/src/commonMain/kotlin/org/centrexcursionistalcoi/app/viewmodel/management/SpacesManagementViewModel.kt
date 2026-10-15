package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.SpaceDetail
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.request.CreateSpaceRequest
import org.centrexcursionistalcoi.app.database.SpaceKeyTypesRepository
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.network.SpaceKeyTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.SpaceKeysRemoteRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.SpacesRemoteRepository
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.viewmodel.launch
import org.centrexcursionistalcoi.app.viewmodel.stateInViewModel
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * For the people who manage spaces: creating and editing them, their keys, and closing them.
 */
@KoinViewModel
class SpacesManagementViewModel(
    spacesRepository: SpacesRepository,
    spaceKeyTypesRepository: SpaceKeyTypesRepository,
    spaceKeysRepository: SpaceKeysRepository,
    private val spacesRemoteRepository: SpacesRemoteRepository,
    private val spaceKeyTypesRemoteRepository: SpaceKeyTypesRemoteRepository,
    private val spaceKeysRemoteRepository: SpaceKeysRemoteRepository,
) : ViewModel() {
    val spaces = spacesRepository.selectAllAsFlow().stateInViewModel()
    val keyTypes = spaceKeyTypesRepository.selectAllAsFlow().stateInViewModel()
    val keys = spaceKeysRepository.selectAllAsFlow().stateInViewModel()

    fun createSpace(draft: SpaceDraft) = launch {
        spacesRemoteRepository.create(
            CreateSpaceRequest(
                name = draft.name,
                description = draft.description,
                conditionsOfUse = draft.conditionsOfUse.takeIf { it.isNotBlank() },
                requiresKeys = draft.requiresKeys,
                prices = draft.prices,
                photos = draft.newPhotos.map { it.fileWithContext() },
                seats = draft.seats,
                beds = draft.beds,
                energySources = draft.energySources,
                climateControl = draft.climateControl,
                address = draft.address.takeIf { it.isNotBlank() },
                latitude = draft.latitude,
                longitude = draft.longitude,
                altitude = draft.altitude,
            ),
        )
    }

    /**
     * Whatever is empty in [draft] is removed from the space.
     */
    fun updateSpace(id: Uuid, draft: SpaceDraft) = launch {
        val photos = draft.removedPhotos.map { FileWithContext(id = it) } + draft.newPhotos.map { it.fileWithContext() }
        spacesRemoteRepository.update(
            id,
            UpdateSpaceRequest(
                name = draft.name,
                description = draft.description,
                conditionsOfUse = draft.conditionsOfUse,
                requiresKeys = draft.requiresKeys,
                prices = draft.prices,
                photos = photos.takeIf { it.isNotEmpty() },
                seats = draft.seats,
                beds = draft.beds,
                energySources = draft.energySources,
                climateControl = draft.climateControl,
                address = draft.address,
                latitude = draft.latitude,
                longitude = draft.longitude,
                altitude = draft.altitude,
                clear = buildSet {
                    if (draft.seats == null) add(SpaceDetail.SEATS)
                    if (draft.beds == null) add(SpaceDetail.BEDS)
                    if (draft.altitude == null) add(SpaceDetail.ALTITUDE)
                    if (draft.latitude == null || draft.longitude == null) add(SpaceDetail.COORDINATES)
                },
            ),
        )
    }

    fun closeSpace(id: Uuid, since: Instant, until: Instant?, reason: String?) = launch {
        spacesRemoteRepository.setClosed(id, true, since, until, reason)
    }

    fun openSpace(id: Uuid) = launch { spacesRemoteRepository.setClosed(id, false) }

    fun deleteSpace(id: Uuid) = launch { spacesRemoteRepository.delete(id) }

    /** Creates a type of key that is for several spaces. */
    fun createKeyType(name: String, description: String?, spaces: List<SpaceKeyTypeSpace>) = launch {
        spaceKeyTypesRemoteRepository.create(name, description, spaces)
    }

    /** Changes a type of key, and all the spaces it is for. An empty [description] removes it. */
    fun updateKeyType(id: Uuid, name: String, description: String, spaces: List<SpaceKeyTypeSpace>) = launch {
        spaceKeyTypesRemoteRepository.update(id, name, description, spaces)
    }

    fun deleteKeyType(id: Uuid) = launch { spaceKeyTypesRemoteRepository.delete(id) }

    fun createKey(type: Uuid, label: String?, nfcId: ByteArray?) = launch {
        spaceKeysRemoteRepository.create(type, label, nfcId)
    }

    /** An empty [label] or [nfcId] removes it. */
    fun updateKey(id: Uuid, label: String, nfcId: ByteArray) = launch {
        spaceKeysRemoteRepository.update(id, label, nfcId)
    }

    fun deleteKey(id: Uuid) = launch { spaceKeysRemoteRepository.delete(id) }
}
