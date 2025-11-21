package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import kotlin.time.Clock
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.ReferencedPost.Companion.referenced
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.DepartmentsRepository
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
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
        link: String?,
        files: List<PlatformFile>,
        progressNotifier: ProgressNotifier
    ) {
        val inMemoryFiles = files.map { InMemoryFileAllocator.put(it) }

        create(
            Post(
                Uuid.Zero,
                Clock.System.now(),
                title,
                content,
                departmentId,
                link,
                inMemoryFiles.map { it.toFileWithContext() },
            ),
            progressNotifier,
        )
    }

    suspend fun update(
        postId: Uuid,
        title: String?,
        content: String?,
        departmentId: Uuid?,
        link: String?,
        files: List<PlatformFile>,
        removedFiles: List<Uuid>,
        progressNotifier: ProgressNotifier
    ) {
        val filesWithContext = files.map { it.fileWithContext() } + removedFiles.map { FileWithContext(byteArrayOf(), id = it) }

        update(
            postId,
            UpdatePostRequest(
                title,
                content,
                departmentId,
                link,
                filesWithContext,
            ),
            UpdatePostRequest.serializer(),
            progressNotifier
        )
    }
}
