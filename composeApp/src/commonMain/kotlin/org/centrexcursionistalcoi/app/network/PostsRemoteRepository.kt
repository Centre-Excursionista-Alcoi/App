package org.centrexcursionistalcoi.app.network

import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.utils.Zero

object PostsRemoteRepository: RemoteRepository<Uuid, Post>(
    "/posts",
    Post.serializer(),
    PostsRepository
) {
    suspend fun create(
        title: String,
        content: String,
        onlyForMembers: Boolean,
        departmentId: Long
    ) = create(
        Post(Uuid.Zero, Clock.System.now(), title, content, onlyForMembers, departmentId)
    )
}
