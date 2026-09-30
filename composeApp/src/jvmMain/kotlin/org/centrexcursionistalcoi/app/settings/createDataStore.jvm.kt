package org.centrexcursionistalcoi.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import io.github.vinceglb.filekit.utils.div
import io.github.vinceglb.filekit.utils.toFile
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.centrexcursionistalcoi.app.di.globalPathsProvider

actual fun createDataStore(): DataStore<Preferences> = createDataStore(
    storage = OkioStorage(
        fileSystem = FileSystem.SYSTEM,
        serializer = PreferencesSerializer,
        producePath = {
            (globalPathsProvider.systemDataPath / dataStoreFileName).toFile().toOkioPath()
        }
    )
)
