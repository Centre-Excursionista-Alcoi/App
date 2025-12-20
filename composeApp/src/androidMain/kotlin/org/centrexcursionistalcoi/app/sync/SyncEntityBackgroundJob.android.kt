package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.WorkerParameters

actual class SyncEntityBackgroundJob(appContext: Context, workerParams: WorkerParameters) : BackgroundSyncWorker<SyncEntityBackgroundJobLogic>(appContext, workerParams) {
    override val logicInstance: SyncEntityBackgroundJobLogic = SyncEntityBackgroundJobLogic
}
