package org.centrexcursionistalcoi.app.database

import com.diamondedge.logging.logging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher

private val log = logging()

interface Repository<T : Entity<IdType>, IdType: Any> {
    fun selectAllAsFlow(dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<List<T>>

    suspend fun selectAll(): List<T>

    suspend fun get(id: IdType): T?

    fun getAsFlow(id: IdType, dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<T?>

    suspend fun insert(item: T): Long

    suspend fun insert(items: List<T>)

    suspend fun update(item: T): Long

    suspend fun update(items: List<T>)

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
     */
    suspend fun deleteAll() {
        val entities = selectAll()
        log.d { "Deleting all ${entities.size} items..." }
        deleteByIdList(entities.map { it.id })
    }
}
