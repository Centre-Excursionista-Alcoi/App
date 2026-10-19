package org.centrexcursionistalcoi.app.settings

import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE

/**
 * Applies the language the user chose, if any.
 *
 * Call it before the first composition: `LocalinaApp` rebuilds everything inside it when the locale changes, so applying
 * the language later (e.g. from a ViewModel that loads in the background) runs the whole startup twice.
 */
suspend fun SettingsStore.applySavedLanguage() {
    get(SETTINGS_LANGUAGE)?.let { LocaleUpdater.updateLocale(it) }
}
