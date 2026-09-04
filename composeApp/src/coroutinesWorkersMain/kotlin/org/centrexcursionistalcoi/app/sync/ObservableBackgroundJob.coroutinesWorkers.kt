package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.uuid.Uuid

actual open class ObservableBackgroundJob(actual val id: Uuid) : KoinComponent {
    actual fun stateFlow(): Flow<BackgroundJobState> = runBlocking {
        get<BackgroundJobCoordinator>().fetchStateFlowById(id)
    }
}
