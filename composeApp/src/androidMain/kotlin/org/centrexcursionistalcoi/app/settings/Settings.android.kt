package org.centrexcursionistalcoi.app.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.datastore.DataStoreSettings

lateinit var dataStore: DataStore<Preferences>

@OptIn(ExperimentalSettingsApi::class, ExperimentalSettingsImplementation::class)
actual val settings: FlowSettings by lazy {
    DataStoreSettings(dataStore)
}
