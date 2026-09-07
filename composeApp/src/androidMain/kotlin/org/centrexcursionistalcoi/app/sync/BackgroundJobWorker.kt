package org.centrexcursionistalcoi.app.sync

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.diamondedge.logging.logging
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.koin.android.annotation.KoinWorker
import org.koin.core.qualifier.named
import org.koin.java.KoinJavaComponent.inject

@KoinWorker
class BackgroundJobWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val logic: BackgroundJob by inject(
        BackgroundJob::class.java,
        named(workerParams.inputData.getString(EXTRA_LOGIC_NAME) ?: throw IllegalArgumentException("Missing logic name in input data"))
    )

    private val log = logging()

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
            progressNotifier = ProgressNotifier { progress ->
                val isIndeterminate = if (progress is Progress.Transfer) {
                    progress.isIndeterminate
                } else {
                    true
                }
                val name = if (progress is Progress.NamedProgress) {
                    progress.name
                } else {
                    null
                }
                setProgress(
                    workDataOf(
                        PROGRESS_KEY_TYPE to progress::class.simpleName,
                        PROGRESS_KEY_IS_INDETERMINATE to isIndeterminate,
                        PROGRESS_KEY_CURRENT to (progress as? Progress.Transfer)?.current,
                        PROGRESS_KEY_TOTAL to (progress as? Progress.Transfer)?.total,
                        PROGRESS_KEY_NAME to name,
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
        return with(logic) {
            try {
                log.d { "Running ${logic::class.simpleName} with input: $input" }
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
        const val EXTRA_LOGIC_NAME = "logic_name"

        const val PROGRESS_KEY_TYPE = "type"
        const val PROGRESS_KEY_IS_INDETERMINATE = "is_indeterminate"
        const val PROGRESS_KEY_CURRENT = "current"
        const val PROGRESS_KEY_TOTAL = "total"
        const val PROGRESS_KEY_NAME = "name"

        const val RESULT_EXCEPTION_TYPE = "exception.type"
        const val RESULT_EXCEPTION_MESSAGE = "exception.message"
        const val RESULT_EXCEPTION_STACKTRACE = "exception.stacktrace"
    }
}
