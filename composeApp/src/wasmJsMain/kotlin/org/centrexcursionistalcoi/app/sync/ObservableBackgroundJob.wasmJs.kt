package org.centrexcursionistalcoi.app.sync

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

actual class ObservableBackgroundJob(
    actual val id: Uuid,
    private val flowProvider: () -> Flow<BackgroundJobState>
) {
    actual fun stateFlow(): Flow<BackgroundJobState> = flowProvider()
}
