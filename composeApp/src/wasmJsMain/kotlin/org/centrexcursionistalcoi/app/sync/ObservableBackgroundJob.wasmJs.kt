package org.centrexcursionistalcoi.app.sync

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

actual open class ObservableBackgroundJob(actual val id: Uuid) {
    actual fun stateFlow(): Flow<BackgroundJobState> = BackgroundJobCoordinator.fetchStateFlowById(id)
}
