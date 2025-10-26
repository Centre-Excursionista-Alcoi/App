package org.centrexcursionistalcoi.app.sync

import androidx.work.WorkInfo
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

actual class ObservableBackgroundJob(id: UUID, private val flowProvider: () -> Flow<WorkInfo>) {
    actual val id: Uuid = id.toKotlinUuid()

    actual fun stateFlow(): Flow<BackgroundJobState> = flowProvider().map { it.state.toBackgroundJobState() }
}
