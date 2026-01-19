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

expect class SyncEntityBackgroundJob : BackgroundSyncWorker<SyncEntityBackgroundJobLogic>

object SyncEntityBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_ENTITY_CLASS = "entity_class"
    const val EXTRA_ENTITY_ID = "entity_id"
    const val EXTRA_IS_DELETE = "is_delete"

    private val log = logging()

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val entityClass = input[EXTRA_ENTITY_CLASS] ?: return SyncResult.Failure("Invalid or missing entity class")
        val entityId = input[EXTRA_ENTITY_ID] ?: return SyncResult.Failure("Invalid or missing entity ID")
        val isDelete = input[EXTRA_IS_DELETE]?.toBoolean() ?: false

        if (isDelete) {
            log.d { "Deleting $entityClass#$entityId..." }
            when (entityClass) {
                Department::class.simpleName -> DepartmentsRepository.delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid department ID: $entityId")
                )
                Post::class.simpleName -> PostsRepository.delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid post ID: $entityId")
                )
                InventoryItemType::class.simpleName -> InventoryItemTypesRepository.delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item type ID: $entityId")
                )
                InventoryItem::class.simpleName -> InventoryItemsRepository.delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item ID: $entityId")
                )
                Event::class.simpleName -> EventsRepository.delete(
                    id = entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid event ID: $entityId")
                )
                else -> log.w { "Got unknown entity class: $entityClass" }
            }
        } else {
            log.d { "Updating $entityClass#$entityId..." }
            when (entityClass) {
                Department::class.simpleName -> DepartmentsRemoteRepository.get(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid department ID: $entityId"),
                    ignoreIfModifiedSince = true
                )?.let { DepartmentsRepository.insertOrUpdate(it) }
                Post::class.simpleName -> PostsRemoteRepository.get(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid post ID: $entityId"),
                    ignoreIfModifiedSince = true
                )?.let { PostsRepository.insertOrUpdate(it) }
                InventoryItemType::class.simpleName -> InventoryItemTypesRemoteRepository.get(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item type ID: $entityId"),
                    ignoreIfModifiedSince = true
                )?.let { InventoryItemTypesRepository.insertOrUpdate(it) }
                InventoryItem::class.simpleName -> InventoryItemsRemoteRepository.get(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid item ID: $entityId"),
                    ignoreIfModifiedSince = true
                )?.let { InventoryItemsRepository.insertOrUpdate(it) }
                Event::class.simpleName -> EventsRemoteRepository.get(
                    entityId.toUuidOrNull() ?: return SyncResult.Failure("Invalid event ID: $entityId"),
                    ignoreIfModifiedSince = true
                )?.let { EventsRepository.insertOrUpdate(it) }
                else -> log.w { "Got unknown entity class: $entityClass" }
            }
        }

        return SyncResult.Success()
    }
}
