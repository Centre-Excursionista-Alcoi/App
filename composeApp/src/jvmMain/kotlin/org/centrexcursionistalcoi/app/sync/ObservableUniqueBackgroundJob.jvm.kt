package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow

actual class ObservableUniqueBackgroundJob(
    actual val name: String,
    val job: ObservableBackgroundJob
) {
    actual fun stateFlow(): Flow<BackgroundJobState> = job.stateFlow()
}
