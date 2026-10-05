package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.data.SpaceKeyTypeSpace
import org.centrexcursionistalcoi.app.database.SpaceKeyTypesRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.CreateSpaceKeyTypeRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceKeyTypeRequest
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_SPACE_KEY_TYPES_SYNC
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class SpaceKeyTypesRemoteRepository(
    private val spaceKeyTypesRepository: SpaceKeyTypesRepository,
) : RemoteRepository<Uuid, SpaceKeyType, Uuid, SpaceKeyType>(
    Api.SpaceKeyTypes.resources,
    SETTINGS_LAST_SPACE_KEY_TYPES_SYNC,
    SpaceKeyType.serializer(),
    spaceKeyTypesRepository,
    remoteToLocalIdConverter = { it },
) {
    suspend fun create(
        name: String,
        description: String?,
        spaces: List<SpaceKeyTypeSpace>,
        progressNotifier: ProgressNotifier? = null,
    ) {
        createJson(
            CreateSpaceKeyTypeRequest(name, description, spaces),
            CreateSpaceKeyTypeRequest.serializer(),
            progressNotifier,
        )
    }

    /**
     * @param description An empty string removes it.
     * @param spaces Replaces all the spaces the type is for.
     */
    suspend fun update(
        id: Uuid,
        name: String?,
        description: String?,
        spaces: List<SpaceKeyTypeSpace>?,
        progressNotifier: ProgressNotifier? = null,
    ) = update(id, UpdateSpaceKeyTypeRequest(name, description, spaces), UpdateSpaceKeyTypeRequest.serializer(), progressNotifier)

    override suspend fun insertRemoteEntity(entity: SpaceKeyType): SpaceKeyType {
        spaceKeyTypesRepository.upsert(entity)
        return entity
    }

    override suspend fun updateRemoteEntity(entity: SpaceKeyType): SpaceKeyType {
        spaceKeyTypesRepository.upsert(entity)
        return entity
    }

    override suspend fun upsertRemoteEntity(entity: SpaceKeyType): SpaceKeyType {
        spaceKeyTypesRepository.upsert(entity)
        return entity
    }
}
