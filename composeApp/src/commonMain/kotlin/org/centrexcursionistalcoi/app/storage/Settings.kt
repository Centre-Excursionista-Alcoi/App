package org.centrexcursionistalcoi.app.storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable

@OptIn(ExperimentalSettingsApi::class)
val settings: ObservableSettings = Settings().makeObservable()

/**
 * Key for storing the selected language in the settings.
 */
const val SETTINGS_LANGUAGE = "language"
