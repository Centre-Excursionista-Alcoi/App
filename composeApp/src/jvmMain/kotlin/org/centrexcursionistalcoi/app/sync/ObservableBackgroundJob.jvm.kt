package org.centrexcursionistalcoi.app.sync

import io.github.aakira.napier.Napier
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

actual open class ObservableBackgroundJob(
    val input: Map<String, String>,
    val requiresInternet: Boolean,
    actual val id: Uuid,
    open val tags: List<String>,
    val uniqueName: String?,
    val repeatInterval: Duration?,
    val logic: BackgroundSyncWorkerLogic,
) {
    private val state = MutableStateFlow(BackgroundJobState.ENQUEUED)

    actual fun stateFlow(): Flow<BackgroundJobState> = state.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            BackgroundJobCoordinator.append(id, tags, uniqueName, this@ObservableBackgroundJob)

            if (repeatInterval != null) {
                while (true) {
                    execute()
                    delay(repeatInterval)
                }
            } else {
                execute()
            }

            BackgroundJobCoordinator.remove(id, tags, uniqueName, this@ObservableBackgroundJob)
        }
    }

    private suspend fun execute() {
        try {
            state.emit(BackgroundJobState.RUNNING)
            with(logic) {
                BackgroundSyncContext().run(input)
            }
            state.emit(BackgroundJobState.SUCCEEDED)
        } catch (e: Throwable) {
            Napier.e(e) { "Job failed." }
            state.emit(BackgroundJobState.FAILED)
        }
    }
}
