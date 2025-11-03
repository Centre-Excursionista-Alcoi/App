package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow

actual class ObservableUniqueBackgroundJob(actual val name: String) {
    actual fun stateFlow(): Flow<BackgroundJobState> = BackgroundJobCoordinator.fetchStateFlowByUniqueName(name)
}
