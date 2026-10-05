package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.SpaceLending
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceLendingEntity.Companion.toEntity
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class SpaceLendingsRepository(db: AppDatabase) : Repository<SpaceLending, Uuid> {
    private val dao = db.spaceLendingDao()

    override suspend fun get(id: Uuid): SpaceLending? = dao.get(id)?.toSpaceLending()

    override suspend fun getByIdList(ids: List<Uuid>): List<SpaceLending> = dao.getByIdList(ids).map { it.toSpaceLending() }

    override fun getAsFlow(id: Uuid): Flow<SpaceLending?> = dao.getAsFlow(id).map { it?.toSpaceLending() }

    override fun selectAllAsFlow(): Flow<List<SpaceLending>> = dao.selectAllAsFlow().map { list -> list.map { it.toSpaceLending() } }

    override suspend fun selectAll(): List<SpaceLending> = dao.selectAll().map { it.toSpaceLending() }

    override suspend fun insert(item: SpaceLending) = dao.upsert(item.toEntity())

    override suspend fun update(item: SpaceLending) = dao.upsert(item.toEntity())

    suspend fun upsert(item: SpaceLending) = dao.upsert(item.toEntity())

    override suspend fun delete(id: Uuid) = dao.deleteById(id)
}
