package org.centrexcursionistalcoi.app.sync

import kotlinx.coroutines.flow.Flow

actual class ObservableBackgroundJobs {
    actual val tag: String
        get() = TODO("Not yet implemented")

    actual fun stateFlow(): Flow<List<BackgroundJobState>> {
        TODO("Not yet implemented")
    }
}
