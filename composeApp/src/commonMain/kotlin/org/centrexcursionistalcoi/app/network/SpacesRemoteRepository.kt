package org.centrexcursionistalcoi.app.network

import org.centrexcursionistalcoi.app.data.CategoryPrice
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.CreateSpaceRequest
import org.centrexcursionistalcoi.app.request.UpdateSpaceRequest
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_SPACES_SYNC
import org.koin.core.annotation.Singleton
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Singleton
class SpacesRemoteRepository(
    private val spacesRepository: SpacesRepository,
) : RemoteRepository<Uuid, Space, Uuid, Space>(
    Api.Spaces.resources,
    SETTINGS_LAST_SPACES_SYNC,
    Space.serializer(),
    spacesRepository,
    remoteToLocalIdConverter = { it },
) {
    suspend fun create(
        name: String,
        description: String,
        conditionsOfUse: String?,
        requiresKeys: Boolean,
        prices: List<CategoryPrice>,
        progressNotifier: ProgressNotifier? = null,
    ) {
        createJson(
            CreateSpaceRequest(name, description, conditionsOfUse, requiresKeys, prices),
            CreateSpaceRequest.serializer(),
            progressNotifier,
        )
    }

    suspend fun update(
        id: Uuid,
        request: UpdateSpaceRequest,
        progressNotifier: ProgressNotifier? = null,
    ) = update(id, request, UpdateSpaceRequest.serializer(), progressNotifier)

    /**
     * Closes or opens a space. Opening it clears the closing information.
     */
    suspend fun setClosed(
        id: Uuid,
        closed: Boolean,
        since: Instant? = null,
        until: Instant? = null,
        reason: String? = null,
        progressNotifier: ProgressNotifier? = null,
    ) = update(
        id,
        UpdateSpaceRequest(isClosed = closed, closedSince = since, closedUntil = until, closedReason = reason),
        UpdateSpaceRequest.serializer(),
        progressNotifier,
    )

    override suspend fun insertRemoteEntity(entity: Space): Space {
        spacesRepository.upsert(entity)
        return entity
    }

    override suspend fun updateRemoteEntity(entity: Space): Space {
        spacesRepository.upsert(entity)
        return entity
    }

    override suspend fun upsertRemoteEntity(entity: Space): Space {
        spacesRepository.upsert(entity)
        return entity
    }
}
