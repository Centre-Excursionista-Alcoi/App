package org.centrexcursionistalcoi.app.sync

import io.github.aakira.napier.Napier
import kotlin.js.Promise
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher

val workerTypes: Map<KClass<out BackgroundSyncWorker<*>>, BackgroundSyncWorkerLogic> = mapOf(
    SyncAllDataBackgroundJob::class to SyncAllDataBackgroundJobLogic,
    SyncLendingBackgroundJob::class to SyncLendingBackgroundJobLogic,
)

@OptIn(ExperimentalWasmJsInterop::class)
actual object BackgroundJobCoordinator {
    var jobs = emptyList<ObservableBackgroundJob>()

    actual suspend inline fun <reified WorkerType : BackgroundSyncWorker<*>> schedule(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: Duration?
    ): ObservableBackgroundJob {
        val flow = MutableStateFlow(BackgroundJobState.ENQUEUED)
        val job = ObservableBackgroundJob(id ?: Uuid.random()) { flow }.also { jobs += it }
        val logic = workerTypes[WorkerType::class] ?: error("No logic registered for worker type ${WorkerType::class}")
        coroutineScope {
            launch {
                try {
                    val context = BackgroundSyncContext(
                        progressNotifier = { Napier.d { "Job progress: $it" } }
                    )
                    flow.emit(BackgroundJobState.RUNNING)
                    with(logic) {
                        context.run(input)
                    }
                    flow.emit(BackgroundJobState.SUCCEEDED)
                } catch (e: Exception) {
                    Napier.e(e) { "Job failed." }
                    flow.emit(BackgroundJobState.FAILED)
                } finally {
                    jobs -= job
                }
            }
        }
        return job
    }

    @OptIn(DelicateCoroutinesApi::class)
    actual inline fun <reified WorkerType : BackgroundSyncWorker<*>> scheduleAsync(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: Duration?
    ) {
        val flow = MutableStateFlow(BackgroundJobState.ENQUEUED)
        val job = ObservableBackgroundJob(id ?: Uuid.random()) { flow }.also { jobs += it }
        val logic = workerTypes[WorkerType::class] ?: error("No logic registered for worker type ${WorkerType::class}")
        Promise { resolve, reject ->
            GlobalScope.launch(defaultAsyncDispatcher) {
                try {
                    val context = BackgroundSyncContext(
                        progressNotifier = { Napier.d { "Job progress: $it" } }
                    )
                    flow.emit(BackgroundJobState.RUNNING)
                    with(logic) {
                        context.run(input)
                    }
                    flow.emit(BackgroundJobState.SUCCEEDED)
                    resolve(null)
                } catch (e: Exception) {
                    Napier.e(e) { "Job failed." }
                    flow.emit(BackgroundJobState.FAILED)
                    reject("$e".toJsString())
                } finally {
                    jobs -= job
                }
            }
        }
    }

    actual fun observe(id: Uuid): ObservableBackgroundJob {
        return jobs.find { it.id == id } ?: throw IllegalArgumentException("Job with ID $id not found")
    }

    actual fun observe(tag: String): ObservableBackgroundJobs {
        TODO("Not yet implemented")
    }

    actual fun observeUnique(name: String): ObservableUniqueBackgroundJob {
        TODO("Not yet implemented")
    }
}
