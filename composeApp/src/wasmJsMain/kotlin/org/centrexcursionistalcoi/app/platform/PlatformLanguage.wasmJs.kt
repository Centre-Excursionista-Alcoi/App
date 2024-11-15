package org.centrexcursionistalcoi.app.platform

actual object PlatformLanguage {
    /**
     * Whether the platform supports changing the app language.
     * If `false` those functions won't do anything:
     * - [changeAppLanguage]
     * Required [isLanguageFetchSupported] to be `true`, otherwise doesn't enable anything.
     */
    actual val isLanguageChangeSupported: Boolean = false

    /**
     * Whether the platform supports fetching the system language.
     * If `false` those functions won't do anything:
     * - [getSelectedLanguage]
     * - [localizedNameForTag]
     */
    actual val isLanguageFetchSupported: Boolean = false

    /**
     * Get the currently selected language.
     */
    actual fun getSelectedLanguage(): String? {
        throw UnsupportedOperationException()
    }

    /**
     * Change the app language to the specified language.
     * @see getSelectedLanguage
     */
    actual fun changeAppLanguage(language: String) {
        throw UnsupportedOperationException()
    }

    /**
     * Get the localized name for the specified language.
     */
    actual fun localizedNameForTag(language: String): String {
        throw UnsupportedOperationException()
    }
}
