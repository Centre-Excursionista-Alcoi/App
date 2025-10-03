package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.SuspendingTransacterImpl
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class Repository<T : Any, IdType: Any> {
    protected abstract val queries: SuspendingTransacterImpl

    abstract fun selectAllAsFlow(dispatcher: CoroutineDispatcher = Dispatchers.Default): Flow<List<T>>

    abstract suspend fun selectAll(): List<T>

    abstract suspend fun insert(item: T): Long

    suspend fun insert(items: List<T>) {
        queries.transaction {
            afterRollback { Napier.w { "No entities were inserted." } }
            afterCommit { Napier.v { "${items.size} entities were inserted." } }

            for (item in items) {
                insert(item)
            }
        }
    }

    abstract suspend fun update(item: T): Long

    suspend fun update(items: List<T>) {
        queries.transaction {
            afterRollback { Napier.w { "No entities were updated." } }
            afterCommit { Napier.v { "${items.size} entities were updated." } }

            for (item in items) {
                update(item)
            }
        }
    }

    abstract suspend fun delete(id: IdType)

    suspend fun deleteByIdList(ids: List<IdType>) {
        queries.transaction {
            afterRollback { Napier.w { "No entities were deleted" } }
            afterCommit { Napier.v { "${ids.size} entities were deleted." } }

            for (id in ids) {
                delete(id)
            }
        }
    }
}
