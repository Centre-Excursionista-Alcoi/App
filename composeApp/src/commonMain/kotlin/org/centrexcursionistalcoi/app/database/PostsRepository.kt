package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.database.room.entity.PostEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.database.room.relation.toReferenced
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class PostsRepository(db: AppDatabase) : Repository<ReferencedPost, Uuid> {
    private val dao = db.postDao()

    override suspend fun get(id: Uuid): ReferencedPost? = dao.get(id)?.toReferenced()

    override fun getAsFlow(id: Uuid): Flow<ReferencedPost?> = dao.getAsFlow(id).map { it?.toReferenced() }

    override fun selectAllAsFlow(): Flow<List<ReferencedPost>> = dao.selectAllAsFlow().map { list -> list.map { it.toReferenced() } }

    override suspend fun selectAll(): List<ReferencedPost> = dao.selectAll().map { it.toReferenced() }

    override suspend fun insert(item: ReferencedPost) = dao.insert(
        item.dereference().toEntity()
    )

    override suspend fun update(item: ReferencedPost) = dao.update(
        item.dereference().toEntity()
    )

    override suspend fun delete(id: Uuid) {
        dao.deleteById(id)
    }
}
