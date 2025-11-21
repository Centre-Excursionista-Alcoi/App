package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.centrexcursionistalcoi.app.data.Department
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.ReferencedPost.Companion.referenced
import org.centrexcursionistalcoi.app.database.data.Posts
import org.centrexcursionistalcoi.app.storage.databaseInstance

object PostsRepository : DatabaseRepository<ReferencedPost, Uuid>() {
    override val queries by lazy { databaseInstance.postsQueries }

    override suspend fun get(id: Uuid): ReferencedPost? {
        val departments = DepartmentsRepository.selectAll()
        return queries.get(id).awaitAsList().firstOrNull()?.toPost(departments)
    }

    override fun getAsFlow(id: Uuid, dispatcher: CoroutineDispatcher): Flow<ReferencedPost?> {
        val departmentsFlow = DepartmentsRepository.selectAllAsFlow(dispatcher)
        val postsFlow = queries.get(id).asFlow().mapToList(dispatcher)
        return combine(departmentsFlow, postsFlow) { departments, posts ->
            posts.firstOrNull()?.toPost(departments)
        }
    }

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher): Flow<List<ReferencedPost>> {
        val departmentsFlow = DepartmentsRepository.selectAllAsFlow(dispatcher)
        val postsFlow = queries.selectAll().asFlow().mapToList(dispatcher)
        return combine(departmentsFlow, postsFlow) { departments, posts ->
            posts.map { it.toPost(departments) }
        }
    }

    override suspend fun selectAll(): List<ReferencedPost> {
        val departments = DepartmentsRepository.selectAll()
        return queries.selectAll().awaitAsList().map { it.toPost(departments) }
    }

    override suspend fun insert(item: ReferencedPost) = queries.insert(
        id = item.id,
        date = item.date,
        title = item.title,
        content = item.content,
        department = item.department?.id,
        link = item.link,
        files = item.files,
    )

    override suspend fun update(item: ReferencedPost) = queries.update(
        id = item.id,
        date = item.date,
        title = item.title,
        content = item.content,
        department = item.department?.id,
        link = item.link,
        files = item.files,
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    fun Posts.toPost(departments: List<Department>) = Post(
        id = id,
        date = date,
        title = title,
        content = content,
        department = department,
        link = link,
        files = files.orEmpty(),
    ).referenced(departments)
}
