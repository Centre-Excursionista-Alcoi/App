package org.centrexcursionistalcoi.app.translation

import java.util.Locale

class TranslationsList(
    val locale: Locale,
    private val translations: Map<String, String>
) {
    operator fun get(key: String) = translations[key] ?: throw NoSuchElementException("Translation $key not found")

    fun getOrNull(key: String, vararg args: Any?) = translations[key]?.format(*args)

    fun get(key: String, vararg args: Any?) = get(key).format(*args)
}
