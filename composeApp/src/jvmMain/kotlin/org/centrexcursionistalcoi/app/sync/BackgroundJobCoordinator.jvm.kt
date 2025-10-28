package org.centrexcursionistalcoi.app.sync

import kotlin.time.Duration
import kotlin.uuid.Uuid

actual object BackgroundJobCoordinator {

    var idJobsList = emptyMap<Uuid, ObservableBackgroundJob>()
    var tagsJobsList = emptyMap<String, List<ObservableBackgroundJob>>()
    val uniqueJobsList = mutableMapOf<String, ObservableUniqueBackgroundJob>()

    actual suspend inline fun <Logic: BackgroundSyncWorkerLogic, reified WorkerType: BackgroundSyncWorker<Logic>> schedule(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: Duration?,
        logic: Logic,
    ): ObservableBackgroundJob {
        return ObservableBackgroundJob(
            input = input,
            requiresInternet = requiresInternet,
            id = id ?: Uuid.random(),
            tags = tags,
            uniqueName = uniqueName,
            repeatInterval = repeatInterval,
            logic = logic
        )
    }

    actual inline fun <Logic: BackgroundSyncWorkerLogic, reified WorkerType: BackgroundSyncWorker<Logic>> scheduleAsync(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: Duration?,
        logic: Logic,
    ) {
        ObservableBackgroundJob(
            input = input,
            requiresInternet = requiresInternet,
            id = id ?: Uuid.random(),
            tags = tags,
            uniqueName = uniqueName,
            repeatInterval = repeatInterval,
            logic = logic
        )
    }

    actual fun observe(id: Uuid): ObservableBackgroundJob {
        return idJobsList[id] ?: throw IllegalArgumentException("No job found with id: $id")
    }

    actual fun observe(tag: String): ObservableBackgroundJobs {
        val jobs = tagsJobsList[tag] ?: throw IllegalArgumentException("No jobs found with tag: $tag")
        return ObservableBackgroundJobs(tag, jobs)
    }

    actual fun observeUnique(name: String): ObservableUniqueBackgroundJob {
        return uniqueJobsList[name] ?: throw IllegalArgumentException("No unique job found with name: $name")
    }
}