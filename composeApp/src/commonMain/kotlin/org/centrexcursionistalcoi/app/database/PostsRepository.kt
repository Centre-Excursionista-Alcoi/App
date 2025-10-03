package org.centrexcursionistalcoi.app.database

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.data.Posts

object PostsRepository : SettingsRepository<Post, Uuid>("posts", Post.serializer()) { // DatabaseRepository<Post, Uuid>()
    /*override val queries by lazy { databaseInstance.postsQueries }

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
    }*/

    private fun Posts.toPost() = Post(
        id = id,
        date = date,
        title = title,
        content = content,
        onlyForMembers = onlyForMembers,
        departmentId = department ?: throw IllegalStateException("Post with id $id has no department ID")
    )
}
