package org.centrexcursionistalcoi.app.translation

import java.util.Locale
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@ExperimentalXmlUtilApi
abstract class Template(namespace: String, name: String) {
    protected val translationsBook = TranslationsBook(namespace, name)

    fun render(locale: Locale, args: Map<String, String?>): String {
        val list = translationsBook[locale]
        val redactor = DocumentRedactor(locale, list)
        return redactor.render(args)
    }

    protected abstract fun DocumentRedactor.render(args: Map<String, String?>): String
}
