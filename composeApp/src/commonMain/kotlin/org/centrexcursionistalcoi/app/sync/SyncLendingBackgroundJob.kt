package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.push.PushNotification
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import kotlin.uuid.Uuid

expect class SyncLendingBackgroundJob : BackgroundSyncWorker<SyncLendingBackgroundJobLogic>

object SyncLendingBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_LENDING_ID = "lending_id"
    const val EXTRA_IS_REMOVAL = "is_removal"

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val lendingId = input[EXTRA_LENDING_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing lending ID")
        val isRemoval = input[EXTRA_IS_REMOVAL]?.toBoolean() ?: false

        if (isRemoval) {
            LendingsRepository.delete(lendingId)
        } else {
            LendingsRemoteRepository.update(lendingId)
                ?: return SyncResult.Failure("Lending with ID $lendingId not found on server")
        }

        return SyncResult.Success()
    }

    fun scheduleAsync(lendingId: Uuid, isRemoval: Boolean) = BackgroundJobCoordinator.scheduleAsync<SyncLendingBackgroundJobLogic, SyncLendingBackgroundJob>(
        input = mapOf(
            EXTRA_LENDING_ID to lendingId.toString(),
            EXTRA_IS_REMOVAL to isRemoval.toString(),
        ),
        logic = SyncLendingBackgroundJobLogic,
    )
}
