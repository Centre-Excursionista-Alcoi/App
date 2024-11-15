package org.centrexcursionistalcoi.app.platform

val languages = arrayOf("en")

expect object PlatformLanguage {
    /**
     * Whether the platform supports changing the app language.
     * If `false` those functions won't do anything:
     * - [changeAppLanguage]
     * Required [isLanguageFetchSupported] to be `true`, otherwise doesn't enable anything.
     */
    val isLanguageChangeSupported: Boolean

    /**
     * Whether the platform supports fetching the system language.
     * If `false` those functions won't do anything:
     * - [getSelectedLanguage]
     * - [localizedNameForTag]
     */
    val isLanguageFetchSupported: Boolean

    /**
     * Get the currently selected language.
     */
    fun getSelectedLanguage(): String?

    /**
     * Change the app language to the specified language.
     * @see getSelectedLanguage
     */
    fun changeAppLanguage(language: String)

    /**
     * Get the localized name for the specified language.
     */
    fun localizedNameForTag(language: String): String
}
