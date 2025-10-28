package org.centrexcursionistalcoi.app.sync

import androidx.compose.runtime.mutableStateMapOf
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual object BackgroundJobCoordinator {

    val idJobsList = mutableStateMapOf<Uuid, ObservableBackgroundJob>()
    val tagsJobsList = mutableStateMapOf<String, List<ObservableBackgroundJob>>()
    val uniqueJobsList = mutableStateMapOf<String, ObservableUniqueBackgroundJob>()

    val mutex = Mutex()

    suspend fun append(id: Uuid, tags: List<String>, uniqueName: String?, job: ObservableBackgroundJob) = mutex.withLock {
        idJobsList += id to job
        for (tag in tags) {
            val currentJobs = tagsJobsList[tag]?.toMutableList() ?: mutableListOf()
            currentJobs += job
            tagsJobsList += tag to currentJobs
        }
        if (uniqueName != null) {
            uniqueJobsList[uniqueName] = ObservableUniqueBackgroundJob(uniqueName, job)
        }
    }

    suspend fun remove(id: Uuid, tags: List<String>, uniqueName: String?, job: ObservableBackgroundJob) = mutex.withLock {
        idJobsList -= id
        for (tag in tags) {
            val currentJobs = tagsJobsList[tag]?.toMutableList()
            currentJobs?.remove(job)
            if (currentJobs.isNullOrEmpty()) {
                tagsJobsList -= tag
            } else {
                tagsJobsList += tag to currentJobs
            }
        }
        if (uniqueName != null) {
            uniqueJobsList.remove(uniqueName)
        }
    }

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
        return runBlocking {
            mutex.withLock {
                idJobsList[id] ?: throw IllegalArgumentException("No job found with id: $id")
            }
        }
    }

    actual fun observe(tag: String): ObservableBackgroundJobs {
        return runBlocking {
            mutex.withLock {
                val jobs = tagsJobsList[tag] ?: throw IllegalArgumentException("No jobs found with tag: $tag")
                ObservableBackgroundJobs(tag, jobs)
            }
        }
    }

    actual fun observeUnique(name: String): ObservableUniqueBackgroundJob {
        return runBlocking {
            mutex.withLock {
                uniqueJobsList[name] ?: throw IllegalArgumentException(
                    "No unique job found with name: $name.\n\tUnique jobs: ${uniqueJobsList.keys.joinToString().ifBlank { "<none>" }}"
                )
            }
        }
    }
}