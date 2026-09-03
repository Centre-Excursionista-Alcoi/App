package org.centrexcursionistalcoi.app.database

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.ReferencedPost.Companion.referenced
import org.centrexcursionistalcoi.app.database.room.entity.PostEntity.Companion.toEntity
import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
class PostsRepository(
    db: AppDatabase,
    private val departmentsRepository: DepartmentsRepository,
) : Repository<ReferencedPost, Uuid> {
    private val dao = db.postDao()

    override suspend fun get(id: Uuid): ReferencedPost? {
        val departments = departmentsRepository.selectAll()
        return dao.get(id)?.toPost()?.referenced(departments)
    }

    override fun getAsFlow(id: Uuid): Flow<ReferencedPost?> {
        val departmentsFlow = departmentsRepository.selectAllAsFlow()
        val postFlow = dao.getAsFlow(id)
        return combine(departmentsFlow, postFlow) { departments, post ->
            post?.toPost()?.referenced(departments)
        }
    }

    override fun selectAllAsFlow(): Flow<List<ReferencedPost>> {
        val departmentsFlow = departmentsRepository.selectAllAsFlow()
        val postsFlow = dao.selectAllAsFlow()
        return combine(departmentsFlow, postsFlow) { departments, posts ->
            posts.map { it.toPost().referenced(departments) }
        }
    }

    override suspend fun selectAll(): List<ReferencedPost> {
        val departments = departmentsRepository.selectAll()
        return dao.selectAll().map { it.toPost().referenced(departments) }
    }

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
