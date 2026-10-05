package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.database.SpaceKeyTypesRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.network.SpaceKeyTypesRemoteRepository
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
    private val spacesRemoteRepository: SpacesRemoteRepository,
    private val spaceKeyTypesRemoteRepository: SpaceKeyTypesRemoteRepository,
) : ViewModel() {
    val spaces = spacesRepository.selectAllAsFlow().stateInViewModel()
    val keyTypes = spaceKeyTypesRepository.selectAllAsFlow().stateInViewModel()

    fun createSpace(
        name: String,
        description: String,
        conditionsOfUse: String?,
        requiresKeys: Boolean,
        prices: List<CategoryPrice>,
    ) = launch {
        spacesRemoteRepository.create(name, description, conditionsOfUse, requiresKeys, prices)
    }

    /**
     * Empty [conditionsOfUse] removes them.
     */
    fun updateSpace(
        id: Uuid,
        name: String,
        description: String,
        conditionsOfUse: String,
        requiresKeys: Boolean,
        prices: List<CategoryPrice>,
    ) = launch {
        spacesRemoteRepository.update(
            id,
            UpdateSpaceRequest(
                name = name,
                description = description,
                conditionsOfUse = conditionsOfUse,
                requiresKeys = requiresKeys,
                prices = prices,
            ),
        )
    }

    fun closeSpace(id: Uuid, since: Instant, until: Instant?, reason: String?) = launch {
        spacesRemoteRepository.setClosed(id, true, since, until, reason)
    }

    fun openSpace(id: Uuid) = launch { spacesRemoteRepository.setClosed(id, false) }

    fun deleteSpace(id: Uuid) = launch { spacesRemoteRepository.delete(id) }

    fun createKeyType(space: Uuid, name: String, maxPerLending: Int) = launch {
        spaceKeyTypesRemoteRepository.create(name, null, listOf(SpaceKeyTypeSpace(space, maxPerLending)))
    }

    /** Changes the name of [type], and how many of it a lending of [space] can take. */
    fun updateKeyType(type: SpaceKeyType, space: Uuid, name: String, maxPerLending: Int) = launch {
        val spaces = type.spaces.filterNot { it.space == space } + SpaceKeyTypeSpace(space, maxPerLending)
        spaceKeyTypesRemoteRepository.update(type.id, name, null, spaces)
    }

    /** Stops [type] being for [space]. The type stays for its other spaces. */
    fun removeKeyType(type: SpaceKeyType, space: Uuid) = launch {
        spaceKeyTypesRemoteRepository.update(type.id, null, null, type.spaces.filterNot { it.space == space })
    }
}
