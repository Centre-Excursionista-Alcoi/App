package storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings

@ExperimentalSettingsApi
expect val settings: ObservableSettings
