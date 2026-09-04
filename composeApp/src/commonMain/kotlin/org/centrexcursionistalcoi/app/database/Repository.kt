package org.centrexcursionistalcoi.app.database

import com.diamondedge.logging.logging
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.exception.MissingCrossReferenceException

private val log = logging()

interface Repository<T : Entity<IdType>, IdType: Any> {
    fun selectAllAsFlow(): Flow<List<T>>

    /**
     * Fetches a list of all the elements of this repository.
     * @throws MissingCrossReferenceException If a reference of the item is not found.
     */
    suspend fun selectAll(): List<T>

    /**
     * Searches for an item with the given [id].
     * @return The item with the given [id], or `null` if it doesn't exist.
     * @throws MissingCrossReferenceException If a reference of the item is not found.
     */
    suspend fun get(id: IdType): T?

    suspend fun getByIdList(ids: List<IdType>): List<T>

    fun getAsFlow(id: IdType): Flow<T?>

    suspend fun insert(item: T)

    suspend fun insert(items: List<T>) {
        for (item in items) {
            insert(item)
        }
    }

    suspend fun update(item: T)

    suspend fun update(items: List<T>) {
        for (item in items) {
            update(item)
        }
    }

    suspend fun delete(id: IdType)

    suspend fun deleteByIdList(ids: List<IdType>) {
        for (id in ids) {
            delete(id)
        }
    }

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
