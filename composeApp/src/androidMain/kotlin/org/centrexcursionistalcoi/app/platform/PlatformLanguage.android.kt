package org.centrexcursionistalcoi.app.platform

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

actual object PlatformLanguage {
    actual val isLanguageChangeSupported: Boolean = true

    actual val isLanguageFetchSupported: Boolean = true

    actual fun getSelectedLanguage(): String? {
        val supportedLanguages = languages.map(Locale::forLanguageTag)

        val list = LocaleListCompat.getDefault().let {
            (0 until it.size()).mapNotNull(it::get)
        }
        return list.find { supportedLanguages.contains(it) }?.toLanguageTag()
    }

    actual fun changeAppLanguage(language: String) {
        val appLocaleList = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocaleList)
    }

    actual fun localizedNameForTag(language: String): String {
        return Locale.forLanguageTag(language).displayName
    }
}
