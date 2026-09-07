package org.centrexcursionistalcoi.app.sync

import org.centrexcursionistalcoi.app.database.LendingsRepository
import org.centrexcursionistalcoi.app.network.LendingsRemoteRepository
import org.centrexcursionistalcoi.app.utils.toUuidOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
@Named(SyncLendingBackgroundJob.NAME)
class SyncLendingBackgroundJob(
    private val lendingsRepository: LendingsRepository,
    private val lendingsRemoteRepository: LendingsRemoteRepository
) : BackgroundJob() {
    override suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult {
        val lendingId = input[EXTRA_LENDING_ID]?.toUuidOrNull()
            ?: return SyncResult.Failure("Invalid or missing lending ID")
        val isRemoval = input[EXTRA_IS_REMOVAL]?.toBoolean() ?: false

        if (isRemoval) {
            lendingsRepository.delete(lendingId)
        } else {
            lendingsRemoteRepository.update(lendingId)
                ?: return SyncResult.Failure("Lending with ID $lendingId not found on server")
        }

        return SyncResult.Success()
    }

    companion object {
        const val NAME = "SyncLendingBackgroundJob"
        const val EXTRA_LENDING_ID = "lending_id"
        const val EXTRA_IS_REMOVAL = "is_removal"
    }
}
