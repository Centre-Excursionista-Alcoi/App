package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.database.Repository

abstract class SymmetricRemoteRepository<IdType : Any, EntityType : Entity<IdType>>(
    endpoint: String,
    lastSyncSettingsKey: String,
    serializer: KSerializer<EntityType>,
    private val repository: Repository<EntityType, IdType>,
    isCreationSupported: Boolean = true,
    isPatchSupported: Boolean = true,
) : RemoteRepository<IdType, EntityType, IdType, EntityType>(
    endpoint,
    lastSyncSettingsKey,
    serializer,
    repository,
    isCreationSupported,
    isPatchSupported,
    remoteToLocalIdConverter = { it },
) {
    // Local and remote entities are the same type here, so no relation-hydration is needed: the fetched
    // entity can be persisted and returned as-is.
    override suspend fun insertRemoteEntity(entity: EntityType): EntityType {
        repository.insert(entity)
        return entity
    }

    override suspend fun updateRemoteEntity(entity: EntityType): EntityType {
        repository.update(entity)
        return entity
    }

    override suspend fun upsertRemoteEntity(entity: EntityType): EntityType {
        if (repository.get(entity.id) == null) repository.insert(entity) else repository.update(entity)
        return entity
    }
}
