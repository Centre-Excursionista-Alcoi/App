package storage

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

private val delegate = Preferences.userRoot()

@ExperimentalSettingsApi
actual val settings: ObservableSettings by lazy {
    PreferencesSettings(delegate)
}
