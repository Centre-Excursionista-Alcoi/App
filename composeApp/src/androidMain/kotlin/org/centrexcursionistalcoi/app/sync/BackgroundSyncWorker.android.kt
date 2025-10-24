package org.centrexcursionistalcoi.app.sync

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.centrexcursionistalcoi.app.process.Progress

actual abstract class BackgroundSyncWorker<Logic : BackgroundSyncWorkerLogic>(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    protected abstract val logicInstance: Logic

    @Suppress("UNCHECKED_CAST")
    @SuppressLint("RestrictedApi")
    fun Result.toSyncResult(): SyncResult = when (this) {
        is Result.Success -> SyncResult.Success(this.outputData.keyValueMap.filterValues { it !is String } as Map<String, String>)
        is Result.Retry -> SyncResult.Retry(this.outputData.keyValueMap.filterValues { it !is String } as Map<String, String>)
        is Result.Failure -> SyncResult.Failure(this.outputData.keyValueMap.filterValues { it !is String } as Map<String, String>)
        else -> throw IllegalStateException("Unknown Result type: ${this::class.simpleName}")
    }

    fun SyncResult.toWorkerResult(): Result = when (this) {
        is SyncResult.Success -> Result.success(workDataOf(*outputData.toList().toTypedArray()))
        is SyncResult.Retry -> Result.retry()
        is SyncResult.Failure -> Result.failure(workDataOf(*outputData.toList().toTypedArray()))
    }

    override suspend fun doWork(): Result {
        val context = BackgroundSyncContext(
            progressNotifier = { progress ->
                val isIndeterminate = if (progress is Progress.Transfer) {
                    progress.isIndeterminate
                } else {
                    true
                }
                setProgress(
                    workDataOf(
                        "type" to progress::class.simpleName,
                        "is_indeterminate" to isIndeterminate,
                        "current" to (progress as? Progress.Transfer)?.current,
                        "total" to (progress as? Progress.Transfer)?.total,
                    )
                )
            }
        )
        @Suppress("UNCHECKED_CAST")
        val input = inputData.keyValueMap.filterValues { it !is String } as Map<String, String>

        @Suppress("UNCHECKED_CAST")
        return with(logicInstance) {
            context.run(input).toWorkerResult()
        }
    }
}
