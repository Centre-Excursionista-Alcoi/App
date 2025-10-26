package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import io.github.aakira.napier.Napier
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlinx.coroutines.flow.mapNotNull

actual object BackgroundJobCoordinator {
    var workManager: WorkManager? = null
        private set

    fun initialize(context: Context) {
        workManager = WorkManager.getInstance(context)
    }

    inline fun <reified WorkerType: BackgroundSyncWorker<*>> enqueueJob(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: UUID?,
        tags: List<String>,
        uniqueName: String?,
    ): Operation {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val request = OneTimeWorkRequestBuilder<WorkerType>()
            .setId(id ?: UUID.randomUUID())
            .apply {
                // Add all the given tags
                for (tag in tags) addTag(tag)
            }
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

        Napier.d { "Scheduling ${WorkerType::class.simpleName} with id $id..." }
        return if (uniqueName != null) {
            workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
        } else {
            workManager.enqueue(request)
        }
    }

    actual suspend inline fun <reified WorkerType: BackgroundSyncWorker<*>> schedule(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
    ): ObservableBackgroundJob {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val id = id?.toJavaUuid() ?: UUID.randomUUID()

        enqueueJob<WorkerType>(input, requiresInternet, id, tags, uniqueName).await()

        return ObservableBackgroundJob(id, flowProvider = { workManager.getWorkInfoByIdFlow(id).mapNotNull { it!! } })
    }

    actual inline fun <reified WorkerType : BackgroundSyncWorker<*>> scheduleAsync(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?
    ) {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val id = id?.toJavaUuid() ?: UUID.randomUUID()

        enqueueJob<WorkerType>(input, requiresInternet, id, tags, uniqueName)
    }

    actual fun observe(id: Uuid): ObservableBackgroundJob {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val id = id.toJavaUuid()
        return ObservableBackgroundJob(id, flowProvider = { workManager.getWorkInfoByIdFlow(id).mapNotNull { it!! } })
    }

    actual fun observe(tag: String): ObservableBackgroundJobs {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        return ObservableBackgroundJobs(tag, flowProvider = { workManager.getWorkInfosByTagFlow(tag) })
    }

    actual fun observeUnique(name: String): ObservableUniqueBackgroundJob {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        return ObservableUniqueBackgroundJob(name, flowProvider = { workManager.getWorkInfosForUniqueWorkFlow(name).mapNotNull { it.firstOrNull() } })
    }
}
