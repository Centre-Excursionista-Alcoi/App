package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

actual class ObservableBackgroundJobs(actual val tag: String, private val jobs: List<ObservableBackgroundJob>) {
    actual fun stateFlow(): Flow<List<BackgroundJobState>> {
        return combine(jobs.map { it.stateFlow() }) { flows ->
            flows.toList()
        }
    }
}
