package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.WorkerParameters

actual class SyncLendingBackgroundJob(appContext: Context, workerParams: WorkerParameters) : BackgroundSyncWorker<SyncLendingBackgroundJobLogic>(appContext, workerParams) {
    override val logicInstance: SyncLendingBackgroundJobLogic = SyncLendingBackgroundJobLogic
}
