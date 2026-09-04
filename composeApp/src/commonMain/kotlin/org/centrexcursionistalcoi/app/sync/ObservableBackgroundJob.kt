package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlin.uuid.Uuid

expect class ObservableBackgroundJob {
    val id: Uuid

    fun stateFlow(): Flow<BackgroundJobState>
}

private class JobDoneThrowable : Throwable("Job has finished")

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
