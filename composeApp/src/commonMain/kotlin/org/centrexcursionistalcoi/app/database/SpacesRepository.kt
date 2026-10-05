package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Space
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceEntity.Companion.toEntity
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class SpacesRepository(db: AppDatabase) : Repository<Space, Uuid> {
    private val dao = db.spaceDao()

    override suspend fun get(id: Uuid): Space? = dao.get(id)?.toSpace()

    override suspend fun getByIdList(ids: List<Uuid>): List<Space> = dao.getByIdList(ids).map { it.toSpace() }

    override fun getAsFlow(id: Uuid): Flow<Space?> = dao.getAsFlow(id).map { it?.toSpace() }

    override fun selectAllAsFlow(): Flow<List<Space>> = dao.selectAllAsFlow().map { list -> list.map { it.toSpace() } }

    override suspend fun selectAll(): List<Space> = dao.selectAll().map { it.toSpace() }

    override suspend fun insert(item: Space) = dao.upsert(item.toEntity())

    override suspend fun update(item: Space) = dao.upsert(item.toEntity())

    suspend fun upsert(item: Space) = dao.upsert(item.toEntity())

    override suspend fun delete(id: Uuid) = dao.deleteById(id)
}
