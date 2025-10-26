package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow

expect class ObservableUniqueBackgroundJob {
    val name: String

    fun stateFlow(): Flow<BackgroundJobState>
}
