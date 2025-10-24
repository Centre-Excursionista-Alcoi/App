package org.centrexcursionistalcoi.app.sync

expect object BackgroundJobCoordinator {
    inline fun <reified WorkerType: BackgroundSyncWorker<*>> schedule(
        input: Map<String, String> = emptyMap(),
        requiresInternet: Boolean = false
    )
}
