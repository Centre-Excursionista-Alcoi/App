package org.centrexcursionistalcoi.app.sync

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

expect class ObservableBackgroundJob {
    val id: Uuid

    fun stateFlow(): Flow<BackgroundJobState>
}
