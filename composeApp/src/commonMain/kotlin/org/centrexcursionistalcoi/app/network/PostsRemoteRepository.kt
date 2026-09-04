package org.centrexcursionistalcoi.app.network

import io.github.vinceglb.filekit.PlatformFile
import org.centrexcursionistalcoi.app.data.FileWithContext
import org.centrexcursionistalcoi.app.data.Post
import org.centrexcursionistalcoi.app.data.ReferencedPost
import org.centrexcursionistalcoi.app.data.fileWithContext
import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.database.entity.PostEntity.Companion.toEntity
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.centrexcursionistalcoi.app.request.UpdatePostRequest
import org.centrexcursionistalcoi.app.storage.InMemoryFileAllocator
import org.centrexcursionistalcoi.app.storage.SETTINGS_LAST_POSTS_SYNC
import org.centrexcursionistalcoi.app.utils.Zero
import org.koin.core.annotation.Singleton
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Singleton
class PostsRemoteRepository(
    private val postsRepository: PostsRepository,
) : RemoteRepository<Uuid, ReferencedPost, Uuid, Post>(
    "/posts",
    SETTINGS_LAST_POSTS_SYNC,
    Post.serializer(),
    postsRepository,
    remoteToLocalIdConverter = { it },
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

    override suspend fun insertRemoteEntity(entity: Post): ReferencedPost {
        postsRepository.insert(entity.toEntity())
        return postsRepository.get(entity.id)!!
    }

    override suspend fun updateRemoteEntity(entity: Post): ReferencedPost {
        postsRepository.update(entity.toEntity())
        return postsRepository.get(entity.id)!!
    }

    override suspend fun upsertRemoteEntity(entity: Post): ReferencedPost {
        postsRepository.upsert(entity.toEntity())
        return postsRepository.get(entity.id)!!
    }
}
