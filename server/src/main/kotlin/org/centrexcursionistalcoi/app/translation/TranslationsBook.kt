package org.centrexcursionistalcoi.app.translation

import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlValue

@ExperimentalXmlUtilApi
class TranslationsBook(
    private val namespace: String,
    private val name: String,
    private val format: XML.Companion = XML
) {
    @Serializable
    class Translation(val name: String, @XmlValue val value: String) {
        fun pair() = name to value
    }

    fun forLocale(locale: Locale): TranslationsList {
        var locale = locale
        var reader = this::class.java.getResourceAsStream("/translate/$namespace/$name-${locale.language}.xml")?.bufferedReader()
        if (reader == null) {
            // Fallback to default locale (en)
            reader = this::class.java.getResourceAsStream("/translate/$namespace/$name.xml")!!.bufferedReader()
            locale = Locale.ENGLISH
        }
        val map = format.decodeFromReader(ListSerializer(Translation.serializer()), KtXmlReader(reader)).associate { it.pair() }
        return TranslationsList(locale, map)
    }

    operator fun get(locale: Locale): TranslationsList = forLocale(locale)
}
