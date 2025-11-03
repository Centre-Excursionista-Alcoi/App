package org.centrexcursionistalcoi.app.sync

actual abstract class BackgroundSyncWorker<Logic : BackgroundSyncWorkerLogic>(
    val logic: Logic
)
