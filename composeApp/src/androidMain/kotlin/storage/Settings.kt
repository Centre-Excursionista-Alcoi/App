package storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.toBlockingObservableSettings
import com.russhwolf.settings.datastore.DataStoreSettings
import org.centrexcursionistalcoi.app.applicationContext
import org.centrexcursionistalcoi.app.dataStore

@ExperimentalSettingsApi
@OptIn(ExperimentalSettingsImplementation::class)
actual val settings: ObservableSettings by lazy {
    DataStoreSettings(applicationContext.dataStore).toBlockingObservableSettings()
}
