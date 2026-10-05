package org.centrexcursionistalcoi.app.viewmodel.management

import androidx.lifecycle.ViewModel
import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
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
    spaceKeysRepository: SpaceKeysRepository,
    private val spacesRemoteRepository: SpacesRemoteRepository,
    private val spaceKeysRemoteRepository: SpaceKeysRemoteRepository,
) : ViewModel() {
    val spaces = spacesRepository.selectAllAsFlow().stateInViewModel()
    val keys = spaceKeysRepository.selectAllAsFlow().stateInViewModel()

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

    fun createKey(space: Uuid, name: String, maxQuantity: Int) = launch {
        spaceKeysRemoteRepository.create(space, name, maxQuantity, nfcId = null)
    }

    fun updateKey(id: Uuid, name: String, maxQuantity: Int) = launch {
        spaceKeysRemoteRepository.update(id, name, maxQuantity, nfcId = null)
    }

    fun deleteKey(id: Uuid) = launch { spaceKeysRemoteRepository.delete(id) }
}
