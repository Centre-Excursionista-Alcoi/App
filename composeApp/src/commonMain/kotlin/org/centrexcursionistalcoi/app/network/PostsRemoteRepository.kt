package org.centrexcursionistalcoi.app.network

import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.utils.Zero

object PostsRemoteRepository: SymmetricRemoteRepository<Uuid, Post>(
    "/posts",
    Post.serializer(),
    PostsRepository
) {
    suspend fun create(
        title: String,
        content: String,
        departmentId: Uuid
    ) = create(
        Post(Uuid.Zero, Clock.System.now(), title, content, departmentId)
    )
}
