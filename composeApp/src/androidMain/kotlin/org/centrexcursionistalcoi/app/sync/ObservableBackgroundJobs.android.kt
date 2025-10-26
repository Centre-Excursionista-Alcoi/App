package org.centrexcursionistalcoi.app.sync

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual class ObservableBackgroundJobs(actual val tag: String, private val flowProvider: () -> Flow<List<WorkInfo>>) {
    actual fun stateFlow(): Flow<List<BackgroundJobState>> = flowProvider().map { list -> list.map { it.state.toBackgroundJobState() } }
}
