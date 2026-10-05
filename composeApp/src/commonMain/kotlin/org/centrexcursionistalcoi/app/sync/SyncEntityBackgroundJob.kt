package org.centrexcursionistalcoi.app.sync

import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.SpaceKeysRepository
import org.centrexcursionistalcoi.app.database.SpaceLendingsRepository
import org.centrexcursionistalcoi.app.database.SpacesRepository
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.network.SpaceKeysRemoteRepository
import org.centrexcursionistalcoi.app.network.SpaceLendingsRemoteRepository
import org.centrexcursionistalcoi.app.network.SpacesRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.get

@Singleton
@Named(SyncEntityBackgroundJob.NAME)
class SyncEntityBackgroundJob : BackgroundJob() {
    private val log = logging()

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val entityClass = input[EXTRA_ENTITY_CLASS] ?: return SyncResult.Failure("Invalid or missing entity class")
        val entityId = input[EXTRA_ENTITY_ID] ?: return SyncResult.Failure("Invalid or missing entity ID")
        val isDelete = input[EXTRA_IS_DELETE]?.toBoolean() ?: false

        if (isDelete) {
            log.d { "Deleting $entityClass#$entityId..." }
            when (entityClass) {
                Department::class.simpleName -> get<DepartmentsRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid department ID: $entityId")
                )
                Post::class.simpleName -> get<PostsRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid post ID: $entityId")
                )
                InventoryItemType::class.simpleName -> get<InventoryItemTypesRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item type ID: $entityId")
                )
                InventoryItem::class.simpleName -> get<InventoryItemsRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item ID: $entityId")
                )
                Event::class.simpleName -> get<EventsRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid event ID: $entityId")
                )
                in names<Space>() -> get<SpacesRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid space ID: $entityId")
                )
                in names<SpaceKey>() -> get<SpaceKeysRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid space key ID: $entityId")
                )
                in names<SpaceLending>() -> get<SpaceLendingsRepository>().delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid space lending ID: $entityId")
                )
                else -> log.w { "Got unknown entity class: $entityClass" }
            }
        } else {
            log.d { "Updating $entityClass#$entityId..." }
            when (entityClass) {
                Department::class.simpleName -> get<DepartmentsRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid department ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                Post::class.simpleName -> get<PostsRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid post ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                InventoryItemType::class.simpleName -> get<InventoryItemTypesRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item type ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                InventoryItem::class.simpleName -> get<InventoryItemsRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                Event::class.simpleName -> get<EventsRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid event ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                in names<Space>() -> get<SpacesRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid space ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                in names<SpaceKey>() -> get<SpaceKeysRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid space key ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                in names<SpaceLending>() -> get<SpaceLendingsRemoteRepository>().update(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid space lending ID: $entityId"),
                    ignoreIfModifiedSince = true
                )
                else -> log.w { "Got unknown entity class: $entityClass" }
            }
        }

        return SyncResult.Success()
    }

    /**
     * The names an entity class can come with: the server names the pushed class after the entity it updated, e.g.
     * `SpaceEntity`, besides the data class.
     */
    private inline fun <reified T : Any> names() = listOfNotNull(T::class.simpleName, T::class.simpleName?.plus("Entity"))

    companion object {
        const val NAME = "SyncEntityBackgroundJob"
        const val EXTRA_ENTITY_CLASS = "entity_class"
        const val EXTRA_ENTITY_ID = "entity_id"
        const val EXTRA_IS_DELETE = "is_delete"
    }
}
