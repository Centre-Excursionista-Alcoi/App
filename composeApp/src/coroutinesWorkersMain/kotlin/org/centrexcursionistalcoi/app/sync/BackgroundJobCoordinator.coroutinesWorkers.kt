package org.centrexcursionistalcoi.app.sync

import androidx.compose.runtime.mutableStateMapOf
import com.diamondedge.logging.logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import kotlin.time.Duration
import kotlin.uuid.Uuid

@Singleton
actual class BackgroundJobCoordinator : KoinComponent {
    val coordinatorLog = logging()

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

    inline fun <reified Logic: BackgroundSyncWorkerLogic> scheduleJob(
        input: Map<String, String>,
        id: Uuid,
        uniqueName: String?,
        repeatInterval: Duration?,
    ) {
        coordinatorLog.info { "Scheduling background job $id (uniqueName=$uniqueName, repeatInterval=$repeatInterval). Input: $input" }
        CoroutineScope(get<DispatcherProvider>().io).launch {
            if (repeatInterval != null) {
                while (true) {
                    execute<Logic>(input, id, uniqueName)
                    emitState(id, uniqueName, BackgroundJobState.ENQUEUED)
                    delay(repeatInterval)
                }
            } else {
                execute<Logic>(input, id, uniqueName)
            }
        }
    }

    suspend inline fun <reified Logic: BackgroundSyncWorkerLogic> execute(
        input: Map<String, String>,
        id: Uuid,
        uniqueName: String?
    ) {
        try {
            emitState(id, uniqueName, BackgroundJobState.RUNNING)
            val logic = get<Logic>(named(Logic::class.simpleName!!))
            with(logic) {
                BackgroundSyncContext().run(input)
            }
            emitState(id, uniqueName, BackgroundJobState.SUCCEEDED)
        } catch (e: Throwable) {
            coordinatorLog.e(e) { "Job failed." }
            emitState(id, uniqueName, BackgroundJobState.FAILED)
        }
    }

    actual suspend inline fun <reified Logic: BackgroundSyncWorkerLogic> schedule(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: Duration?,
    ): ObservableBackgroundJob {
        val id = id ?: Uuid.random()
        scheduleJob<Logic>(input, id, uniqueName, repeatInterval)

        return observe(id)
    }

    actual inline fun <reified Logic: BackgroundSyncWorkerLogic> scheduleAsync(
        input: Map<String, String>,
        requiresInternet: Boolean,
        id: Uuid?,
        tags: List<String>,
        uniqueName: String?,
        repeatInterval: Duration?,
    ) {
        scheduleJob<Logic>(input, id ?: Uuid.random(), uniqueName, repeatInterval)
    }

    actual fun observe(id: Uuid): ObservableBackgroundJob {
        return ObservableBackgroundJob(id)
    }

    actual fun observeUnique(name: String): ObservableUniqueBackgroundJob {
        return ObservableUniqueBackgroundJob(name)
    }
}