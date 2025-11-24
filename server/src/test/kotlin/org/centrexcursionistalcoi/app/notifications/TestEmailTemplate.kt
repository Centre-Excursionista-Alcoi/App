package org.centrexcursionistalcoi.app.notifications

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import org.centrexcursionistalcoi.app.translation.DocumentRedactor

@OptIn(ExperimentalXmlUtilApi::class)
class TestEmailTemplate {
    @Test
    fun `test render`() {
        val template = object : EmailTemplate("example") {
            override fun DocumentRedactor.render(args: Map<String, String?>): String = """
            <html>
                <body>
                    <p>${t("greeting", args["userName"])}</p>
                    <p>${t("message")}</p>
                </body>
            </html>
            """.trimIndent()
        }
        assertEquals(
            """
            <html>
                <body>
                    <p>Welcome, Alice</p>
                    <p>This is a test email.</p>
                </body>
            </html>
            """.trimIndent(),
            template.render(Locale.ENGLISH, mapOf("userName" to "Alice"))
        )
        assertEquals(
            """
            <html>
                <body>
                    <p>Benvingut, Josep</p>
                    <p>Aquest és un correu electrònic de prova.</p>
                </body>
            </html>
            """.trimIndent(),
            template.render(Locale.forLanguageTag("ca"), mapOf("userName" to "Josep"))
        )
    }
}
