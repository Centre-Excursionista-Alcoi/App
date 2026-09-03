package org.centrexcursionistalcoi.app.sync

import org.koin.core.component.KoinComponent

abstract class BackgroundSyncWorkerLogic : KoinComponent {
    abstract suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult
}
