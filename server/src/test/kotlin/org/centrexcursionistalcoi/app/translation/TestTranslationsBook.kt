package org.centrexcursionistalcoi.app.translation

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi

@OptIn(ExperimentalXmlUtilApi::class)
class TestTranslationsBook {
    @Test
    fun test() {
        val book = TranslationsBook("test", "example")
        // English
        book[Locale.ENGLISH].let { list ->
            assertEquals("Value", list["key"])
            assertEquals("Value string", list.get("key_string", "string"))
            assertEquals("Value 10", list.get("key_string", 10))
        }
        // Catalan
        book[Locale.forLanguageTag("ca")].let { list ->
            assertEquals("Valor", list["key"])
            assertEquals("Valor string", list.get("key_string", "string"))
            assertEquals("Valor 10", list.get("key_string", 10))
        }
        // Fallback to English
        assertEquals(Locale.ENGLISH, book[Locale.FRANCE].locale)
    }
}
