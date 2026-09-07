package org.centrexcursionistalcoi.app.sync

import androidx.work.WorkInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.centrexcursionistalcoi.app.process.Progress
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

actual class ObservableBackgroundJob(id: UUID, private val flowProvider: () -> Flow<WorkInfo>) {
    actual val id: Uuid = id.toKotlinUuid()

    actual fun stateFlow(): Flow<BackgroundJobState> = flowProvider().map { it.state.toBackgroundJobState() }

    actual fun progressStateFlow(): Flow<Progress> = flowProvider().map { it.progress.toProgress() }
}
