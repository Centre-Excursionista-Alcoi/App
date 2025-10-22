package org.centrexcursionistalcoi.app.database

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
}
