package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.*
import org.centrexcursionistalcoi.app.network.*
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

expect class SyncEntityBackgroundJob : BackgroundSyncWorker<SyncEntityBackgroundJobLogic>

object SyncEntityBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_PATH = "path"

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val path = input[EXTRA_PATH] ?: return SyncResult.Failure("Invalid or missing path")
        val pieces = path.trim('/').split('/')
        if (pieces.size < 2) return SyncResult.Failure("Invalid path")

        val namespace = pieces.subList(0, pieces.size - 1).joinToString("/")
        val id = pieces.last()
        when (namespace) {
            "departments" -> DepartmentsRemoteRepository.get(
                id.toUuidOrNull() ?: return SyncResult.Failure("Invalid department ID: $id")
            )?.let { DepartmentsRepository.insertOrUpdate(it) }
            "posts" -> PostsRemoteRepository.get(
                id.toUuidOrNull() ?: return SyncResult.Failure("Invalid post ID: $id")
            )?.let { PostsRepository.insertOrUpdate(it) }
            "inventory/types" -> InventoryItemTypesRemoteRepository.get(
                id.toUuidOrNull() ?: return SyncResult.Failure("Invalid inventory item type ID: $id")
            )?.let { InventoryItemTypesRepository.insertOrUpdate(it) }
            "inventory/items" -> InventoryItemsRemoteRepository.get(
                id.toUuidOrNull() ?: return SyncResult.Failure("Invalid inventory item ID: $id")
            )?.let { InventoryItemsRepository.insertOrUpdate(it) }
            "events" -> EventsRemoteRepository.get(
                id.toUuidOrNull() ?: return SyncResult.Failure("Invalid event ID: $id")
            )?.let { EventsRepository.insertOrUpdate(it) }
        }

        return SyncResult.Success()
    }
}
