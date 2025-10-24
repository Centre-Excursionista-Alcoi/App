package org.centrexcursionistalcoi.app.database

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import org.centrexcursionistalcoi.app.data.Entity
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher

interface Repository<T : Entity<IdType>, IdType: Any> {
    fun selectAllAsFlow(dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<List<T>>

    suspend fun selectAll(): List<T>

    suspend fun get(id: IdType): T?

    fun getAsFlow(id: IdType, dispatcher: CoroutineDispatcher = defaultAsyncDispatcher): Flow<T?>

    suspend fun insert(item: T): Long

    suspend fun insert(items: List<T>)

    suspend fun update(item: T): Long

    suspend fun update(items: List<T>)

    suspend fun delete(id: IdType)

    suspend fun deleteByIdList(ids: List<IdType>)

    /**
     * Deletes all entries in the repository.
     */
    suspend fun deleteAll() {
        val entities = selectAll()
        Napier.d { "Deleting all ${entities.size} items..." }
        deleteByIdList(entities.map { it.id })
    }
}
