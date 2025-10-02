package org.centrexcursionistalcoi.app.database

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.data.Posts
import org.centrexcursionistalcoi.app.storage.databaseInstance

object PostsRepository : Repository<Post, Uuid> {
    private val queries = databaseInstance.postsQueries

    override fun selectAllAsFlow(dispatcher: CoroutineDispatcher) = queries
        .selectAll()
        .asFlow()
        .mapToList(dispatcher)
        .map { list -> list.map { it.toPost() } }

    override suspend fun selectAll(): List<Post> = queries.selectAll().awaitAsList()
        .map { it.toPost() }

    override suspend fun insert(item: Post) = queries.insert(
        id = item.id,
        date = item.date,
        title = item.title,
        content = item.content,
        onlyForMembers = item.onlyForMembers,
        department = item.departmentId
    )

    override suspend fun update(item: Post) = queries.update(
        id = item.id,
        date = item.date,
        title = item.title,
        content = item.content,
        onlyForMembers = item.onlyForMembers,
        department = item.departmentId
    )

    override suspend fun delete(id: Uuid) {
        queries.deleteById(id)
    }

    private fun Posts.toPost() = Post(
        id = id,
        date = date,
        title = title,
        content = content,
        onlyForMembers = onlyForMembers,
        departmentId = department ?: throw IllegalStateException("Post with id $id has no department ID")
    )
}
