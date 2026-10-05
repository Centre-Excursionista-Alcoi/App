package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.SpaceKey
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity
import org.centrexcursionistalcoi.app.database.entity.SpaceKeyEntity.Companion.toEntity
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class SpaceKeysRepository(db: AppDatabase) : Repository<SpaceKey, Uuid> {
    private val dao = db.spaceKeyDao()

    override suspend fun get(id: Uuid): SpaceKey? = dao.get(id)?.toSpaceKey()

    override suspend fun getByIdList(ids: List<Uuid>): List<SpaceKey> = dao.getByIdList(ids).map { it.toSpaceKey() }

    override fun getAsFlow(id: Uuid): Flow<SpaceKey?> = dao.getAsFlow(id).map { it?.toSpaceKey() }

    override fun selectAllAsFlow(): Flow<List<SpaceKey>> = dao.selectAllAsFlow().map { list -> list.map { it.toSpaceKey() } }

    override suspend fun selectAll(): List<SpaceKey> = dao.selectAll().map { it.toSpaceKey() }

    override suspend fun insert(item: SpaceKey) = dao.upsert(item.toEntity())

    override suspend fun update(item: SpaceKey) = dao.upsert(item.toEntity())

    suspend fun upsert(item: SpaceKey) = dao.upsert(item.toEntity())

    override suspend fun delete(id: Uuid) = dao.deleteById(id)

    fun getByTypeAsFlow(type: Uuid): Flow<List<SpaceKey>> = dao.getByTypeAsFlow(type).map { list -> list.map { it.toSpaceKey() } }
}
