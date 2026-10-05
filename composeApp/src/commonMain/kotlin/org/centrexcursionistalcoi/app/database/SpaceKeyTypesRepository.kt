package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.SpaceKeyType
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyTypeEntity.Companion.toEntity
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class SpaceKeyTypesRepository(db: AppDatabase) : Repository<SpaceKeyType, Uuid> {
    private val dao = db.spaceKeyTypeDao()

    override suspend fun get(id: Uuid): SpaceKeyType? = dao.get(id)?.toSpaceKeyType()

    override suspend fun getByIdList(ids: List<Uuid>): List<SpaceKeyType> = dao.getByIdList(ids).map { it.toSpaceKeyType() }

    override fun getAsFlow(id: Uuid): Flow<SpaceKeyType?> = dao.getAsFlow(id).map { it?.toSpaceKeyType() }

    override fun selectAllAsFlow(): Flow<List<SpaceKeyType>> = dao.selectAllAsFlow().map { list -> list.map { it.toSpaceKeyType() } }

    override suspend fun selectAll(): List<SpaceKeyType> = dao.selectAll().map { it.toSpaceKeyType() }

    override suspend fun insert(item: SpaceKeyType) = dao.upsert(item.toEntity())

    override suspend fun update(item: SpaceKeyType) = dao.upsert(item.toEntity())

    suspend fun upsert(item: SpaceKeyType) = dao.upsert(item.toEntity())

    override suspend fun delete(id: Uuid) = dao.deleteById(id)
}
