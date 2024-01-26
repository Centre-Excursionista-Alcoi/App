package storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.ObservableSettings

@ExperimentalSettingsApi
actual val settings: ObservableSettings by lazy {
    NSUserDefaultsSettings(TODO(""))
}
