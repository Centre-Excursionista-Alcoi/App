package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.PostsRepository
import org.centrexcursionistalcoi.app.network.PostsRemoteRepository
import org.centrexcursionistalcoi.app.push.LocalNotifications
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

expect class SyncPostBackgroundJob : BackgroundSyncWorker<SyncPostBackgroundJobLogic>

object SyncPostBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_POST_ID = "post_id"

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val postId = input[EXTRA_POST_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing post ID")

        val post = PostsRemoteRepository.get(postId, progressNotifier)
            ?: return SyncResult.Failure("Post with ID $postId not found on server")
        PostsRepository.insertOrUpdate(post)

        LocalNotifications.showNotification(
            { post.title },
            { post.content },
            mapOf("postId" to postId.toString())
        )

        return SyncResult.Success()
    }
}
