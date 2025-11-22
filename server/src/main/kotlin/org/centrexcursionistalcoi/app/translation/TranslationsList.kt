package org.centrexcursionistalcoi.app.translation

import java.util.Locale

class TranslationsList(
    val locale: Locale,
    private val translations: Map<String, String>
) {
    operator fun get(key: String) = translations[key] ?: throw NoSuchElementException("Translation $key not found")

    fun get(key: String, vararg args: Any?) = get(key).format(*args)
}
