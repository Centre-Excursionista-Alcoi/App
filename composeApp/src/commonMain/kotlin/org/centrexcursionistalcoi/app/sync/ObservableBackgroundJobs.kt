package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow

expect class ObservableBackgroundJobs {
    val tag: String

    fun stateFlow(): Flow<List<BackgroundJobState>>
}
