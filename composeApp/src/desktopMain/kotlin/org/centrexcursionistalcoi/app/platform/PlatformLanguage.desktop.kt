package org.centrexcursionistalcoi.app.platform

import java.util.Locale

actual object PlatformLanguage {
    actual val isLanguageChangeSupported: Boolean = true

    actual val isLanguageFetchSupported: Boolean = true

    actual fun getSelectedLanguage(): String? {
        return Locale.getDefault().toLanguageTag()
    }

    actual fun changeAppLanguage(language: String) {
        Locale.setDefault(Locale.forLanguageTag(language))
    }

    actual fun localizedNameForTag(language: String): String {
        return Locale.forLanguageTag(language).displayName
    }
}
