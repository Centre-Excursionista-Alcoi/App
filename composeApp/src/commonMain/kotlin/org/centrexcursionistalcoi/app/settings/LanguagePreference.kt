package org.centrexcursionistalcoi.app.settings

import com.diamondedge.logging.logging
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import org.centrexcursionistalcoi.app.database.ProfileRepository
import org.centrexcursionistalcoi.app.network.PreferencesRemoteRepository
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.centrexcursionistalcoi.app.ui.screen.languageFromCode
import org.koin.core.annotation.Singleton

/**
 * The language of the app. It is kept in the app, and the server gets it too, because it sends emails in it: whatever
 * can't be done with the server is left as it is in the app.
 */
@Singleton
class LanguagePreference(
    private val settings: SettingsStore,
    private val remote: PreferencesRemoteRepository,
    private val profileRepository: ProfileRepository,
) {
    private val log = logging()

    /**
     * Changes the language of the app to [code] (a code of an available language), and tells the server. If the server
     * can't be told, only the app changes.
     */
    suspend fun change(code: String) {
        settings.set(SETTINGS_LANGUAGE, code)
        LocaleUpdater.updateLocale(code)
        remote.setLanguage(code)
    }

    /**
     * If the app has no language of its own yet, uses the one the server has for the user, if it has one that the app
     * has. A language the app already has is kept.
     */
    suspend fun restoreFromServerIfUnset() {
        if (settings.get(SETTINGS_LANGUAGE) != null) return
        if (!profileRepository.isLoggedIn()) return

        // The server has the tag of the device, like es-ES
        val code = remote.getLanguage()?.substringBefore('-')?.lowercase()?.takeIf { languageFromCode(it) != null } ?: return
        // It could have been chosen while the server answered
        if (settings.get(SETTINGS_LANGUAGE) != null) return

        log.i { "Using the language of the server: $code" }
        settings.set(SETTINGS_LANGUAGE, code)
        LocaleUpdater.updateLocale(code)
    }
}
