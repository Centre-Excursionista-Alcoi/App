package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.WorkerParameters

actual class SyncAllDataBackgroundJob(appContext: Context, workerParams: WorkerParameters) : BackgroundSyncWorker<SyncAllDataBackgroundJobLogic>(appContext, workerParams) {
    override val logicInstance: SyncAllDataBackgroundJobLogic = SyncAllDataBackgroundJobLogic
}
