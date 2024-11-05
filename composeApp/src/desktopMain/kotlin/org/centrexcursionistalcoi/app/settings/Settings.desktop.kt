package org.centrexcursionistalcoi.app.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import okio.Path.Companion.toPath
import org.centrexcursionistalcoi.app.fs.dataDir

fun createDataStore() = PreferenceDataStoreFactory.createWithPath {
    (dataDir() / "settings.preferences_pb").absolutePathString().toPath()
}

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual val settings: FlowSettings by lazy {
    DataStoreSettings(createDataStore())
}
