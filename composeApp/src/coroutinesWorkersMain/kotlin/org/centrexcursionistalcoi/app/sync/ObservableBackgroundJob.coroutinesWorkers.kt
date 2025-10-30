package org.centrexcursionistalcoi.app.sync

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

actual open class ObservableBackgroundJob(actual val id: Uuid) {
    actual fun stateFlow(): Flow<BackgroundJobState> = runBlocking {
        BackgroundJobCoordinator.fetchStateFlowById(id)
    }
}
