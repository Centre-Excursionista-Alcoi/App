package org.centrexcursionistalcoi.app.sync

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual class ObservableUniqueBackgroundJob(actual val name: String, private val flowProvider: () -> Flow<WorkInfo>) {
    actual fun stateFlow(): Flow<BackgroundJobState> = flowProvider().map { it.state.toBackgroundJobState() }
}
