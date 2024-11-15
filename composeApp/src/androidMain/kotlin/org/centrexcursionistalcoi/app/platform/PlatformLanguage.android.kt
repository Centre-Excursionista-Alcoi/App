package org.centrexcursionistalcoi.app.platform

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

actual object PlatformLanguage {
    actual val isLanguageChangeSupported: Boolean = true

    actual fun getSelectedLanguage(): String? {
        return LocaleListCompat.getDefault()[0]?.toLanguageTag()
    }

    actual fun changeAppLanguage(language: String) {
        val appLocaleList = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocaleList)
    }

    actual fun localizedNameForTag(language: String): String {
        return Locale.forLanguageTag(language).displayName
    }
}
