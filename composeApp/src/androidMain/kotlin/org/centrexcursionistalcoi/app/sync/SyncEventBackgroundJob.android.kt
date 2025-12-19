package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.WorkerParameters

actual class SyncEventBackgroundJob(appContext: Context, workerParams: WorkerParameters) : BackgroundSyncWorker<SyncEventBackgroundJobLogic>(appContext, workerParams) {
    override val logicInstance: SyncEventBackgroundJobLogic = SyncEventBackgroundJobLogic
}
