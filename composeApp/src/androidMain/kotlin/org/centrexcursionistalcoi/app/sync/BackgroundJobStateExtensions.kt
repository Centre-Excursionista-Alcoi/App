package org.centrexcursionistalcoi.app.sync

import androidx.work.WorkInfo

/**
 * Converts WorkManager's [WorkInfo.State] to shared [BackgroundJobState].
 */
fun WorkInfo.State.toBackgroundJobState(): BackgroundJobState = when (this) {
    WorkInfo.State.ENQUEUED -> BackgroundJobState.ENQUEUED
    WorkInfo.State.RUNNING -> BackgroundJobState.RUNNING
    WorkInfo.State.SUCCEEDED -> BackgroundJobState.SUCCEEDED
    WorkInfo.State.FAILED -> BackgroundJobState.FAILED
    WorkInfo.State.BLOCKED -> BackgroundJobState.BLOCKED
    WorkInfo.State.CANCELLED -> BackgroundJobState.CANCELLED
}
