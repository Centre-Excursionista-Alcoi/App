package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

actual class ObservableUniqueBackgroundJob(actual val name: String) {
    actual fun stateFlow(): Flow<BackgroundJobState> = runBlocking {
        BackgroundJobCoordinator.fetchStateFlowByUniqueName(name)
    }
}
