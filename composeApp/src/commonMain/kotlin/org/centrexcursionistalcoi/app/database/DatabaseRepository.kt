package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.SuspendingTransacterImpl
import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.data.Entity

abstract class DatabaseRepository<T : Entity<IdType>, IdType: Any>: Repository<T, IdType> {
    private val log = logging()

    protected abstract val queries: SuspendingTransacterImpl

    override suspend fun insert(items: List<T>) {
        queries.transaction {
            afterRollback { log.w { "No entities were inserted." } }
            afterCommit { log.v { "${items.size} entities were inserted." } }

            for (item in items) {
                insert(item)
            }
        }
    }

    override suspend fun update(items: List<T>) {
        queries.transaction {
            afterRollback { log.w { "No entities were updated." } }
            afterCommit { log.v { "${items.size} entities were updated." } }

            for (item in items) {
                update(item)
            }
        }
    }

    override suspend fun deleteByIdList(ids: List<IdType>) {
        queries.transaction {
            afterRollback { log.w { "No entities were deleted" } }
            afterCommit { log.v { "${ids.size} entities were deleted." } }

            for (id in ids) {
                delete(id)
            }
        }
    }
}
