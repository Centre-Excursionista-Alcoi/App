package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.SuspendingTransacterImpl
import io.github.aakira.napier.Napier
import org.centrexcursionistalcoi.app.data.Entity

abstract class DatabaseRepository<T : Entity<IdType>, IdType: Any>: Repository<T, IdType> {
    protected abstract val queries: SuspendingTransacterImpl

    override suspend fun insert(items: List<T>) {
        queries.transaction {
            afterRollback { Napier.w { "No entities were inserted." } }
            afterCommit { Napier.v { "${items.size} entities were inserted." } }

            for (item in items) {
                insert(item)
            }
        }
    }

    override suspend fun update(items: List<T>) {
        queries.transaction {
            afterRollback { Napier.w { "No entities were updated." } }
            afterCommit { Napier.v { "${items.size} entities were updated." } }

            for (item in items) {
                update(item)
            }
        }
    }

    override suspend fun deleteByIdList(ids: List<IdType>) {
        queries.transaction {
            afterRollback { Napier.w { "No entities were deleted" } }
            afterCommit { Napier.v { "${ids.size} entities were deleted." } }

            for (id in ids) {
                delete(id)
            }
        }
    }
}
