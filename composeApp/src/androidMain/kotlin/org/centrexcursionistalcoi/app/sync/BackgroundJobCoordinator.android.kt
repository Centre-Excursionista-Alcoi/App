package org.centrexcursionistalcoi.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.workDataOf
import io.github.aakira.napier.Napier
import java.time.Duration
import java.util.UUID
import kotlin.time.toJavaDuration
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
        repeatInterval: Duration?,
    ): Operation {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val builder = if (repeatInterval != null) {
            PeriodicWorkRequestBuilder<WorkerType>(repeatInterval)
        } else {
            OneTimeWorkRequestBuilder<WorkerType>()
        }

        val request = builder
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
            when (request) {
                is PeriodicWorkRequest -> {
                    workManager.enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.UPDATE, request)
                }

                is OneTimeWorkRequest -> {
                    workManager.enqueueUniqueWork(uniqueName, ExistingWorkPolicy.REPLACE, request)
                }

                else -> {
                    error("Unsupported work request type: ${request::class}")
                }
            }
        } else {
            workManager.enqueue(request)
        }
    }

    actual suspend inline fun <Logic: BackgroundSyncWorkerLogic, reified WorkerType: BackgroundSyncWorker<Logic>> schedule(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: kotlin.time.Duration?,
        logic: Logic,
    ): ObservableBackgroundJob {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val id = id?.toJavaUuid() ?: UUID.randomUUID()

        enqueueJob<WorkerType>(input, requiresInternet, id, tags, uniqueName, repeatInterval?.toJavaDuration()).await()

        return ObservableBackgroundJob(id, flowProvider = { workManager.getWorkInfoByIdFlow(id).mapNotNull { it!! } })
    }

    actual inline fun <Logic: BackgroundSyncWorkerLogic, reified WorkerType: BackgroundSyncWorker<Logic>> scheduleAsync(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: kotlin.time.Duration?,
        logic: Logic,
    ) {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val id = id?.toJavaUuid() ?: UUID.randomUUID()

        enqueueJob<WorkerType>(input, requiresInternet, id, tags, uniqueName, repeatInterval?.toJavaDuration())
    }

    actual fun observe(id: Uuid): ObservableBackgroundJob {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        val id = id.toJavaUuid()
        return ObservableBackgroundJob(id, flowProvider = { workManager.getWorkInfoByIdFlow(id).mapNotNull { it!! } })
    }

    actual fun observeUnique(name: String): ObservableUniqueBackgroundJob {
        val workManager = workManager
        require(workManager != null) { "Coordinator not initialized." }

        return ObservableUniqueBackgroundJob(name, flowProvider = { workManager.getWorkInfosForUniqueWorkFlow(name).mapNotNull { it.firstOrNull() } })
    }
}
