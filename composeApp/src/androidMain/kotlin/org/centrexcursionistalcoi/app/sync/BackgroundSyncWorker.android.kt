package org.centrexcursionistalcoi.app.sync

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.process.Progress

private val log = logging()

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
        val input = inputData.keyValueMap.keys
            .mapNotNull { key ->
                inputData.getString(key)?.let { key to it }
            }
            .toMap()

        @Suppress("UNCHECKED_CAST")
        return with(logicInstance) {
            try {
                log.d { "Running ${logicInstance::class.simpleName} with input: $input" }
                log.d { "Input data: ${inputData.keyValueMap.keys}" }

                context.run(input).toWorkerResult()
            } catch (e: Exception) {
                log.e(e) { "Worker failed." }
                Result.failure(
                    workDataOf(
                        RESULT_EXCEPTION_TYPE to e::class.simpleName,
                        RESULT_EXCEPTION_MESSAGE to e.message,
                        RESULT_EXCEPTION_STACKTRACE to e.stackTraceToString(),
                    )
                )
            }
        }
    }


    companion object {
        const val RESULT_EXCEPTION_TYPE = "exception.type"
        const val RESULT_EXCEPTION_MESSAGE = "exception.message"
        const val RESULT_EXCEPTION_STACKTRACE = "exception.stacktrace"
    }
}
