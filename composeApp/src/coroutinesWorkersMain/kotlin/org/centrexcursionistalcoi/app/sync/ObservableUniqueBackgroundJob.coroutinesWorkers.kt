package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual class ObservableUniqueBackgroundJob(actual val name: String) : KoinComponent {
    actual fun stateFlow(): Flow<BackgroundJobState> = runBlocking {
        get<BackgroundJobCoordinator>().fetchStateFlowByUniqueName(name)
    }
}
