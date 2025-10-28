package org.centrexcursionistalcoi.app.sync

import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.sync.BackgroundJobCoordinator.schedule

expect object BackgroundJobCoordinator {
    /**
     * Schedules a new unique job with for the given [WorkerType].
     * @param input Any arguments that the job may need.
     * @param requiresInternet If `true`, the job coordinator will run the job only if Internet access is available.
     * @param id An optional id for the job. Otherwise, a random one will be set.
     * @param tags All the tags to add to the job.
     * @param uniqueName If not `null`, a unique job will be scheduled. This will force only one instance of this job to be running at any time.
     * If a request is made, and another job is already running or scheduled with this name, it will be overridden.
     * @param repeatInterval If not `null`, the job will be scheduled to repeat at the given interval.
     * @param logic The logic instance to use for this job.
     * @return An [ObservableBackgroundJob] that allows to watch the job status.
     */
    suspend inline fun <Logic: BackgroundSyncWorkerLogic, reified WorkerType: BackgroundSyncWorker<Logic>> schedule(
        input: Map<String, String> = emptyMap(),
        requiresInternet: Boolean = false,
        id: Uuid? = null,
        tags: List<String> = emptyList(),
        uniqueName: String? = null,
        repeatInterval: kotlin.time.Duration? = null,
        logic: Logic,
    ): ObservableBackgroundJob

    /**
     * Schedules a new unique job with for the given [WorkerType], but unline [schedule], it doesn't wait for the scheduling to complete, it hopes for the best.
     * @param input Any arguments that the job may need.
     * @param requiresInternet If `true`, the job coordinator will run the job only if Internet access is available.
     * @param id An optional id for the job. Otherwise, a random one will be set.
     * @param tags All the tags to add to the job.
     * @param uniqueName If not `null`, a unique job will be scheduled. This will force only one instance of this job to be running at any time.
     * If a request is made, and another job is already running or scheduled with this name, it will be overridden.
     * @param repeatInterval If not `null`, the job will be scheduled to repeat at the given interval.
     * @param logic The logic instance to use for this job.
     */
    inline fun <Logic: BackgroundSyncWorkerLogic, reified WorkerType: BackgroundSyncWorker<Logic>> scheduleAsync(
        input: Map<String, String> = emptyMap(),
        requiresInternet: Boolean = false,
        id: Uuid? = null,
        tags: List<String> = emptyList(),
        uniqueName: String? = null,
        repeatInterval: kotlin.time.Duration? = null,
        logic: Logic,
    )

    /**
     * Retrieves an [ObservableBackgroundJob] for a job with the given [id].
     * @return An [ObservableBackgroundJob] that allows to watch the job status.
     */
    fun observe(id: Uuid): ObservableBackgroundJob

    /**
     * Retrieves an [ObservableUniqueBackgroundJob] for a job with a given unique [name].
     * @return An [ObservableUniqueBackgroundJob] that allows to watch the job status.
     */
    fun observeUnique(name: String): ObservableUniqueBackgroundJob
}
