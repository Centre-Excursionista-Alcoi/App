package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.CreateSpaceKeyRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceKeyRequest
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_SPACE_KEYS_SYNC
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class SpaceKeysRemoteRepository(
    private val spaceKeysRepository: SpaceKeysRepository,
) : RemoteRepository<Uuid, SpaceKey, Uuid, SpaceKey>(
    Api.SpaceKeys.resources,
    SETTINGS_LAST_SPACE_KEYS_SYNC,
    SpaceKey.serializer(),
    spaceKeysRepository,
    remoteToLocalIdConverter = { it },
) {
    suspend fun create(
        space: Uuid,
        name: String,
        maxQuantity: Int,
        nfcId: ByteArray?,
        progressNotifier: ProgressNotifier? = null,
    ) {
        createJson(
            CreateSpaceKeyRequest(space, name, maxQuantity, nfcId),
            CreateSpaceKeyRequest.serializer(),
            progressNotifier,
        )
    }

    /**
     * @param nfcId An empty array removes the NFC tag of the key.
     */
    suspend fun update(
        id: Uuid,
        name: String?,
        maxQuantity: Int?,
        nfcId: ByteArray?,
        progressNotifier: ProgressNotifier? = null,
    ) = update(id, UpdateSpaceKeyRequest(name, maxQuantity, nfcId), UpdateSpaceKeyRequest.serializer(), progressNotifier)

    override suspend fun insertRemoteEntity(entity: SpaceKey): SpaceKey {
        spaceKeysRepository.upsert(entity)
        return entity
    }

    override suspend fun updateRemoteEntity(entity: SpaceKey): SpaceKey {
        spaceKeysRepository.upsert(entity)
        return entity
    }

    override suspend fun upsertRemoteEntity(entity: SpaceKey): SpaceKey {
        spaceKeysRepository.upsert(entity)
        return entity
    }
}
