package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.WorkerParameters

actual class SyncDepartmentBackgroundJob(appContext: Context, workerParams: WorkerParameters) : BackgroundSyncWorker<SyncDepartmentBackgroundJobLogic>(appContext, workerParams) {
    override val logicInstance: SyncDepartmentBackgroundJobLogic = SyncDepartmentBackgroundJobLogic
}
