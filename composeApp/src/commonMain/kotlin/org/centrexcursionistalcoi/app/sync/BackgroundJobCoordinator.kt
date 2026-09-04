package org.centrexcursionistalcoi.app.sync

import org.koin.core.annotation.Singleton
import kotlin.uuid.Uuid

@Singleton
expect class BackgroundJobCoordinator {
    /**
     * Schedules a new unique job with for the given [Logic].
     * @param name [Logic]'s own Koin qualifier (its `NAME` constant, e.g. [SyncEventBackgroundJob.NAME]). Must match
     * the `@Named` qualifier [Logic] is registered under -- it cannot be derived from [Logic] at runtime, since the
     * class name is not stable across minified (R8/ProGuard) builds.
     * @param input Any arguments that the job may need.
     * @param requiresInternet If `true`, the job coordinator will run the job only if Internet access is available.
     * @param id An optional id for the job. Otherwise, a random one will be set.
     * @param tags All the tags to add to the job.
     * @param uniqueName If not `null`, a unique job will be scheduled. This will force only one instance of this job to be running at any time.
     * If a request is made, and another job is already running or scheduled with this name, it will be overridden.
     * @param repeatInterval If not `null`, the job will be scheduled to repeat at the given interval.
     * @return An [ObservableBackgroundJob] that allows to watch the job status.
     */
    suspend inline fun <reified Logic: BackgroundJob> schedule(
        name: String,
        input: Map<String, String> = emptyMap(),
        requiresInternet: Boolean = false,
        id: Uuid? = null,
        tags: List<String> = emptyList(),
        uniqueName: String? = null,
        repeatInterval: kotlin.time.Duration? = null,
    ): ObservableBackgroundJob

    /**
     * Schedules a new unique job with for the given [Logic], but unline [schedule], it doesn't wait for the scheduling to complete, it hopes for the best.
     * @param name [Logic]'s own Koin qualifier (its `NAME` constant, e.g. [SyncEventBackgroundJob.NAME]). Must match
     * the `@Named` qualifier [Logic] is registered under -- it cannot be derived from [Logic] at runtime, since the
     * class name is not stable across minified (R8/ProGuard) builds.
     * @param input Any arguments that the job may need.
     * @param requiresInternet If `true`, the job coordinator will run the job only if Internet access is available.
     * @param id An optional id for the job. Otherwise, a random one will be set.
     * @param tags All the tags to add to the job.
     * @param uniqueName If not `null`, a unique job will be scheduled. This will force only one instance of this job to be running at any time.
     * If a request is made, and another job is already running or scheduled with this name, it will be overridden.
     * @param repeatInterval If not `null`, the job will be scheduled to repeat at the given interval.
     */
    inline fun <reified Logic: BackgroundJob> scheduleAsync(
        name: String,
        input: Map<String, String> = emptyMap(),
        requiresInternet: Boolean = false,
        id: Uuid? = null,
        tags: List<String> = emptyList(),
        uniqueName: String? = null,
        repeatInterval: kotlin.time.Duration? = null,
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
