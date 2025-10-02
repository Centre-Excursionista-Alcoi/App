package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

interface Repository<T : Any, IdType: Any> {
    fun selectAllAsFlow(dispatcher: CoroutineDispatcher = Dispatchers.Default): Flow<List<T>>

    suspend fun selectAll(): List<T>

    suspend fun insert(item: T): Long

    suspend fun update(item: T): Long

    suspend fun delete(id: IdType)

    suspend fun deleteByIdList(ids: List<IdType>) {
        for (id in ids) {
            delete(id)
        }
    }
}
