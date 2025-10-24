package org.centrexcursionistalcoi.app.sync

abstract class BackgroundSyncWorkerLogic {
    abstract suspend fun BackgroundSyncContext.run(input: Map<String, String>): SyncResult
}
