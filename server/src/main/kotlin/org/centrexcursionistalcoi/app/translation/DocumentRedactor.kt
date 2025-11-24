package org.centrexcursionistalcoi.app.translation

import java.util.Locale

class DocumentRedactor(val locale: Locale, private val list: TranslationsList) {
    fun t(key: String, vararg formatArgs: Any?) = list.get(key, *formatArgs)
}
