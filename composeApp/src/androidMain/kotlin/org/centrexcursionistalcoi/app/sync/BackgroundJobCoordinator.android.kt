package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

actual object BackgroundJobCoordinator {
    var workManager: WorkManager? = null
        private set

    fun initialize(context: Context) {
        workManager = WorkManager.getInstance(context)
    }

    actual inline fun <reified WorkerType: BackgroundSyncWorker<*>> schedule(
        input: Map<String, String>,
        requiresInternet: Boolean
    ) {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val request = OneTimeWorkRequestBuilder<WorkerType>()
            .setInputData(workDataOf(*input.toList().toTypedArray()))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        if (requiresInternet) {
                            NetworkType.CONNECTED
                        } else {
                            NetworkType.NOT_REQUIRED
                        }
                    )
                    .build()
            )
            .build()

        workManager.enqueue(request)
    }
}
