package org.centrexcursionistalcoi.app.sync

import androidx.annotation.VisibleForTesting
import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.database.AppDatabase
import org.centrexcursionistalcoi.app.database.InventoryItemTypesRepository
import org.centrexcursionistalcoi.app.database.UsersRepository
import org.centrexcursionistalcoi.app.database.entity.InventoryItemTypeEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.relation.toReferenced
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException
import org.centrexcursionistalcoi.app.network.InventoryItemTypesRemoteRepository
import org.centrexcursionistalcoi.app.network.UsersRemoteRepository
import org.koin.core.annotation.Singleton

@Singleton
class DatabaseIntegrityVerifier(
    private val db: AppDatabase,

    private val inventoryItemTypesRemoteRepository: InventoryItemTypesRemoteRepository,

    private val inventoryItemTypesRepository: InventoryItemTypesRepository,

    private val usersRemoteRepository: UsersRemoteRepository,

    private val usersRepository: UsersRepository,

    private val backgroundJobCoordinator: BackgroundJobCoordinator
) {
    private val log = logging()

    private val inventoryItemDao = db.inventoryItemDao()
    private val memoryDao = db.memoryDao()
    private val lendingDao = db.lendingDao()

    /**
     * Verifies the integrity of the database by checking for missing cross-references and fixing them if possible.
     * @throws IllegalStateException if the database is in an inconsistent state and cannot be fixed.
     */
    suspend fun verifyAndFixReferences() {
        verifyAndFixInventoryItemTypesCrossReferences()
        verifyAndFixMemoriesCrossReferences()
        verifyAndFixLendingsCrossReferences()
    }

    /**
     * Clears the local database entirely and schedules a forced full resync, waiting until it completes, then
     * restarts the whole verification process. Used whenever a missing cross-reference can't be repaired
     * record-by-record.
     * @throws IllegalStateException if the resync job doesn't finish successfully.
     */
    private suspend fun clearDatabaseAndResync() {
        db.clearAllTables()
        val result = backgroundJobCoordinator.schedule<SyncAllDataBackgroundJob>(
            name = SyncAllDataBackgroundJob.UNIQUE_NAME,
            input = mapOf(SyncAllDataBackgroundJob.EXTRA_FORCE_SYNC to "true"),
            requiresInternet = true,
            uniqueName = SyncAllDataBackgroundJob.UNIQUE_NAME,
        ).await()
        if (result != BackgroundJobState.SUCCEEDED) {
            throw IllegalStateException("Failed to sync data after clearing database: $result")
        }
        // The sync was successful, restart the verification process to ensure all references are now valid.
        verifyAndFixReferences()
    }

    /**
     * Tries to recover from a missing "User" cross-reference by fetching [sub] from the server and inserting it
     * locally. Falls back to [clearDatabaseAndResync] if the server doesn't have it either.
     * @return `true` if the database was wiped and fully resynced -- the caller should stop iterating its now-stale
     * list -- or `false` if the missing user was found and inserted, so it's safe to keep going.
     */
    private suspend fun recoverMissingUser(sub: String): Boolean {
        val user = usersRemoteRepository.get(sub)
        return if (user != null) {
            usersRepository.insert(user)
            false
        } else {
            clearDatabaseAndResync()
            true
        }
    }

    /**
     * Verifies the integrity of the inventory item types cross-references and fixes them if possible.
     * @throws IllegalStateException if the database is in an inconsistent state and cannot be fixed.
     */
    @VisibleForTesting
    suspend fun verifyAndFixInventoryItemTypesCrossReferences() {
        for (item in inventoryItemDao.selectAll()) {
            try {
                // Try converting to referenced, which will throw if the type reference is missing
                item.toReferenced()
            } catch (e: MissingCrossReferenceException) {
                log.w(e) { "Missing cross-reference detected for item ${item.item.id}" }
                // There's a missing reference, try fetching it from the server and inserting it into the database
                val type = item.type
                if (type == null) {
                    // If the type is null, something is really wrong, so we can't fix easily.
                    clearDatabaseAndResync()
                    // Return to avoid continuing the loop, as the database has been cleared and re-populated.
                    return
                }

                // We know the item that is missing, try to fetch it from the server and insert it into the database.
                val itemType = inventoryItemTypesRemoteRepository.get(type.type.id)
                if (itemType == null) {
                    // If the type is not found on the server, it's possible that the server has been updated, try fetching the inventory item again, to see if it has been updated to a new type.
                    val updatedItem = inventoryItemDao.get(item.item.id)
                    if (updatedItem == null) {
                        // If the item is not found, something is really wrong, throw an exception to indicate that the database is in an inconsistent state.
                        throw IllegalStateException("Inventory item ${item.item.id} is missing from the database, and the type ${type.type.id} is also missing from the server.")
                    }
                    // If the item is found, check whether the id of the type has been updated
                    if (updatedItem.item.type == type.type.id) {
                        // The type has not been updated, and is still missing from the server, throw an exception to indicate that the database is in an inconsistent state.
                        throw IllegalStateException("Inventory item ${item.item.id} is missing from the database, and the type ${type.type.id} is also missing from the server.")
                    }
                    val updatedItemType = updatedItem.type
                    if (updatedItemType == null) {
                        // The type has been updated to null, which is not allowed, throw an exception to indicate that the database is in an inconsistent state.
                        throw IllegalStateException("Inventory item ${item.item.id} has been updated to a null type, which is not allowed.")
                    }
                    // The type has been updated, try to fetch the new type from the server and insert it into the database
                    val newItemType = inventoryItemTypesRemoteRepository.get(updatedItemType.type.id)
                    if (newItemType == null) {
                        // If the new type is not found on the server, throw an exception to indicate that the database is in an inconsistent state.
                        throw IllegalStateException("Inventory item ${item.item.id} has been updated to a new type ${updatedItem.type}, but the new type is missing from the server.")
                    }
                    inventoryItemTypesRepository.insert(newItemType.toEntity())
                } else {
                    inventoryItemTypesRepository.insert(itemType.toEntity())
                }
            }
        }
    }

    /**
     * Verifies the integrity of the memories cross-references and fixes them if possible.
     *
     * The only required reference a memory has is its submitter -- if that user is missing locally, it's fetched
     * from the server and re-inserted.
     * @throws IllegalStateException if the database is in an inconsistent state and cannot be fixed.
     */
    @VisibleForTesting
    suspend fun verifyAndFixMemoriesCrossReferences() {
        for (memory in memoryDao.selectAll()) {
            try {
                memory.toReferenced()
            } catch (e: MissingCrossReferenceException) {
                log.w(e) { "Missing cross-reference detected for memory ${memory.memory.id}" }
                if (recoverMissingUser(e.id)) return
            }
        }
    }

    /**
     * Verifies the integrity of the lendings cross-references and fixes them if possible.
     *
     * The only required reference a lending has directly is its borrower -- if that user is missing locally, it's
     * fetched from the server and re-inserted. Its items reuse the InventoryItems table already fixed by
     * [verifyAndFixInventoryItemTypesCrossReferences], so any other missing reference bubbling up from them is
     * unexpected and falls back to a full resync.
     * @throws IllegalStateException if the database is in an inconsistent state and cannot be fixed.
     */
    @VisibleForTesting
    suspend fun verifyAndFixLendingsCrossReferences() {
        for (lending in lendingDao.selectAll()) {
            try {
                lending.toReferenced()
            } catch (e: MissingCrossReferenceException) {
                log.w(e) { "Missing cross-reference detected for lending ${lending.lending.id}" }
                val wiped = if (e.type == "User") {
                    recoverMissingUser(e.id)
                } else {
                    clearDatabaseAndResync()
                    true
                }
                if (wiped) return
            }
        }
    }
}
