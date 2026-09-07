package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.push.LocalNotifications
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
@Named(SyncPostBackgroundJob.NAME)
class SyncPostBackgroundJob(
    private val postsRemoteRepository: PostsRemoteRepository
) : BackgroundJob() {
    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val postId = input[EXTRA_POST_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing post ID")

        val post = postsRemoteRepository.update(postId, progressNotifier)
            ?: return SyncResult.Failure("Post with ID $postId not found on server")

        LocalNotifications.showNotification(
            { post.title },
            { post.content },
            mapOf("postId" to postId.toString())
        )

        return SyncResult.Success()
    }

    companion object {
        const val NAME = "SyncPostBackgroundJob"
        const val EXTRA_POST_ID = "post_id"
    }
}
