package org.centrexcursionistalcoi.app.sync

import androidx.compose.runtime.mutableStateMapOf
import io.github.aakira.napier.Napier
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

actual object BackgroundJobCoordinator {

    private var jobStateIdFlows = mapOf<Uuid, MutableStateFlow<BackgroundJobState>>()
    private val jobStateIdMutex = Mutex()

    private var jobStateUniqueNameFlows = mutableStateMapOf<String, MutableStateFlow<BackgroundJobState>>()
    private val jobStateUniqueNameMutex = Mutex()

    suspend fun emitStateById(id: Uuid, state: BackgroundJobState) {
        jobStateIdMutex.withLock {
            val flow = jobStateIdFlows[id] ?: run {
                val newFlow = MutableStateFlow(state)
                jobStateIdFlows = jobStateIdFlows + (id to newFlow)
                newFlow
            }
            flow.emit(state)
        }
    }

    suspend fun fetchStateFlowById(id: Uuid): MutableStateFlow<BackgroundJobState> {
        return jobStateIdMutex.withLock {
            jobStateIdFlows[id] ?: run {
                val newFlow = MutableStateFlow(BackgroundJobState.ENQUEUED)
                jobStateIdFlows = jobStateIdFlows + (id to newFlow)
                newFlow
            }
        }
    }

    suspend fun emitStateByUniqueName(uniqueName: String, state: BackgroundJobState) {
        jobStateUniqueNameMutex.withLock {
            val flow = jobStateUniqueNameFlows[uniqueName] ?: run {
                val newFlow = MutableStateFlow(state)
                jobStateUniqueNameFlows[uniqueName] = newFlow
                newFlow
            }
            flow.emit(state)
        }
    }

    suspend fun fetchStateFlowByUniqueName(uniqueName: String): MutableStateFlow<BackgroundJobState> {
        return jobStateUniqueNameMutex.withLock {
            jobStateUniqueNameFlows[uniqueName] ?: run {
                val newFlow = MutableStateFlow(BackgroundJobState.ENQUEUED)
                jobStateUniqueNameFlows[uniqueName] = newFlow
                newFlow
            }
        }
    }

    suspend fun emitState(id: Uuid, uniqueName: String?, state: BackgroundJobState) {
        emitStateById(id, state)
        if (uniqueName != null) {
            emitStateByUniqueName(uniqueName, state)
        }
    }

    fun scheduleJob(
        input: Map<String, String>,
        id: Uuid,
        uniqueName: String?,
        repeatInterval: Duration?,
        logic: BackgroundSyncWorkerLogic,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            if (repeatInterval != null) {
                while (true) {
                    execute(input, id, uniqueName, logic)
                    emitState(id, uniqueName, BackgroundJobState.ENQUEUED)
                    delay(repeatInterval)
                }
            } else {
                execute(input, id, uniqueName, logic)
            }
        }
    }

    private suspend fun execute(
        input: Map<String, String>,
        id: Uuid,
        uniqueName: String?,
        logic: BackgroundSyncWorkerLogic,
    ) {
        try {
            emitState(id, uniqueName, BackgroundJobState.RUNNING)
            with(logic) {
                BackgroundSyncContext().run(input)
            }
            emitState(id, uniqueName, BackgroundJobState.SUCCEEDED)
        } catch (e: Throwable) {
            Napier.e(e) { "Job failed." }
            emitState(id, uniqueName, BackgroundJobState.FAILED)
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
        val id = id ?: Uuid.random()
        scheduleJob(input, id, uniqueName, repeatInterval, logic)

        return observe(id)
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
        scheduleJob(input, id ?: Uuid.random(), uniqueName, repeatInterval, logic)
    }

    actual fun observe(id: Uuid): ObservableBackgroundJob {
        return ObservableBackgroundJob(id)
    }

    actual fun observeUnique(name: String): ObservableUniqueBackgroundJob {
        return ObservableUniqueBackgroundJob(name)
    }
}