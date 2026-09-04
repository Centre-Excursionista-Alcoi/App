package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.Uuid

expect class ObservableBackgroundJob {
    val id: Uuid

    fun stateFlow(): Flow<BackgroundJobState>
    fun progressStateFlow(): Flow<Progress>
}

private class JobDoneThrowable : Throwable("Job has finished")

fun ObservableBackgroundJob.copyToProgress(
    notifier: ProgressNotifier,
    context: CoroutineContext,
): ObservableBackgroundJob = also {
    CoroutineScope(context).launch {
        progressStateFlow().collect { progress ->
            notifier(progress)
        }
    }
}

/**
 * Blocks the current thread until the job is finished ([BackgroundJobState.isFinished])
 */
suspend fun ObservableBackgroundJob.await(): BackgroundJobState {
    var finalState: BackgroundJobState? = null
    try {
        stateFlow().cancellable().collect { state ->
            if (state.isFinished) {
                finalState = state
                throw JobDoneThrowable()
            }
        }
    } catch (_: JobDoneThrowable) {
        // Job finished, finalState is set
        return finalState ?: throw IllegalStateException("Job finished but no final state was emitted")
    }
    throw IllegalStateException("Job observer exited uncontrollably")
}
