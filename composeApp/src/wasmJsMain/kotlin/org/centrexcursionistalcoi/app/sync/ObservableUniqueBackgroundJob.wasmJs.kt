package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow

actual class ObservableUniqueBackgroundJob {
    actual val name: String
        get() = TODO("Not yet implemented")

    actual fun stateFlow(): Flow<BackgroundJobState> {
        TODO("Not yet implemented")
    }
}