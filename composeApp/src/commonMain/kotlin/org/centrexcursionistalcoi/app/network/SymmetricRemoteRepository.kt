package org.centrexcursionistalcoi.app.network

import kotlinx.serialization.KSerializer
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.database.Repository

abstract class SymmetricRemoteRepository<IdType : Any, EntityType : Entity<IdType>>(
    endpoint: String,
    lastSyncSettingsKey: String,
    serializer: KSerializer<EntityType>,
    repository: Repository<EntityType, IdType>,
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
    remoteToLocalEntityConverter = { it }
)
