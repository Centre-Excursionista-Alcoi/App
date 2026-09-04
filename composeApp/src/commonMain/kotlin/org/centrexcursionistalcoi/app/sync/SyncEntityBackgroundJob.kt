package org.centrexcursionistalcoi.app.sync

import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Event
import org.centrexcursionistalcoi.app.data.InventoryItem
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.EventsRepository
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.InventoryItemsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.network.DepartmentsRemoteRepository
import org.centrexcursionistalcoi.app.network.EventsRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.InventoryItemsRemoteRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.get

@Singleton
@Named("SyncEntityBackgroundJob")
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
                else -> log.w { "Got unknown entity class: $entityClass" }
            }
        }

        return SyncResult.Success()
    }

    companion object {
        const val EXTRA_ENTITY_CLASS = "entity_class"
        const val EXTRA_ENTITY_ID = "entity_id"
        const val EXTRA_IS_DELETE = "is_delete"
    }
}
