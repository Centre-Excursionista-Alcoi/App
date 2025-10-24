package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull

expect class SyncLendingBackgroundJob : BackgroundSyncWorker<SyncLendingBackgroundJobLogic>

object SyncLendingBackgroundJobLogic : BackgroundSyncWorkerLogic() {
    const val EXTRA_LENDING_ID = "lending_id"

    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val lendingId = input[EXTRA_LENDING_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing lending ID")

        val lending = LendingsRemoteRepository.get(lendingId, progressNotifier)
            ?: return SyncResult.Failure("Lending with ID $lendingId not found on server")
        LendingsRepository.insertOrUpdate(lending)

        return SyncResult.Success()
    }
}
