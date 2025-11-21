package org.centrexcursionistalcoi.app.network

import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.ReferencedPost.Companion.referenced
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.utils.Zero

object PostsRemoteRepository: RemoteRepository<Uuid, ReferencedPost, Uuid, Post>(
    "/posts",
    Post.serializer(),
    PostsRepository,
    remoteToLocalIdConverter = { it },
    remoteToLocalEntityConverter = { post ->
        val departments = DepartmentsRepository.selectAll()
        post.referenced(departments)
    },
) {
    suspend fun create(
        title: String,
        content: String,
        departmentId: Uuid?,
        progressNotifier: ProgressNotifier
    ) = create(Post(Uuid.Zero, Clock.System.now(), title, content, departmentId), progressNotifier,)

    suspend fun update(
        postId: Uuid,
        title: String?,
        content: String?,
        departmentId: Uuid?,
        progressNotifier: ProgressNotifier
    ) = update(postId, UpdatePostRequest(title, content, departmentId), UpdatePostRequest.serializer(), progressNotifier)
}
