package org.centrexcursionistalcoi.app.database

import com.diamondedge.logging.logging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher

private val log = logging()

interface Repository<T : Entity<IdType>, IdType: Any> {
    fun selectAllAsFlow(dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<List<T>>

    /**
     * Fetches a list of all the elements of this repository.
     * @throws NoSuchElementException If a reference of any item is not found.
     */
    suspend fun selectAll(): List<T>

    /**
     * Searches for an item with the given [id].
     * @return The item with the given [id], or `null` if it doesn't exist.
     * @throws NoSuchElementException If a reference of the item is not found.
     */
    suspend fun get(id: IdType): T?

    fun getAsFlow(id: IdType, dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<T?>

    suspend fun insert(item: T): Long

    suspend fun insert(items: List<T>)

    suspend fun update(item: T): Long

    suspend fun update(items: List<T>)

    /**
     * Inserts or updates the given [item] in the repository.
     *
     * If an item already exists with the id of [item], it will be updated. Otherwise. it will be inserted.
     * @param item The item to insert/update.
     * @throws NoSuchElementException If the item exists, and it has a missing reference.
     */
    suspend fun insertOrUpdate(item: T) {
        val existingItem = get(item.id)
        if (existingItem == null) {
            insert(item)
        } else {
            update(item)
        }
    }

    suspend fun delete(id: IdType)

    suspend fun deleteByIdList(ids: List<IdType>)

    /**
     * Deletes all entries in the repository.
     * @throws NoSuchElementException If a reference of any item is not found.
     */
    suspend fun deleteAll() {
        val entities = selectAll()
        log.d { "Deleting all ${entities.size} items..." }
        deleteByIdList(entities.map { it.id })
    }
}
