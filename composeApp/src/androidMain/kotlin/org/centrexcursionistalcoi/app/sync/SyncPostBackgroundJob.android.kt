package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.WorkerParameters

actual class SyncPostBackgroundJob(appContext: Context, workerParams: WorkerParameters) : BackgroundSyncWorker<SyncPostBackgroundJobLogic>(appContext, workerParams) {
    override val logicInstance: SyncPostBackgroundJobLogic = SyncPostBackgroundJobLogic
}
