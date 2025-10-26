package org.centrexcursionistalcoi.app.security

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.ResourcesUtils
import org.centrexcursionistalcoi.app.integration.FEMECV

class TestFEMECV {
    @Test
    fun test_getLicenses() = runTest {
        try {
            FEMECV.engine = MockEngine { request ->
                when (request.url.encodedPath) {
                    "/FormLogin.php" -> {
                        respond(
                            content = "",
                            status = HttpStatusCode.Found,
                            headers = headersOf(HttpHeaders.Location, "/PanellControlUsuariFederat.php")
                        )
                    }
                    "/PanellControlUsuariFederat.php" -> {
                        respond(
                            content = ResourcesUtils.bytesFromResource("/femecv/licencias.html"),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
                        )
                    }
                    "/FormLlicencia.php" -> {
                        respond(
                            content = ResourcesUtils.bytesFromResource("/femecv/licencia.html"),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Html.toString())
                        )
                    }
                    "/print/printAutoritzacioLlicencia.php" -> {
                        respond(
                            content = ByteArray(16) { it.toByte() },
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                        )
                    }
                    else -> {
                        respond(
                            content = "Not Found",
                            status = HttpStatusCode.NotFound
                        )
                    }
                }

            }

            val licenses = FEMECV.getLicenses("mail@example.com", "password123")
            assertEquals(1, licenses.size)
            val (license) = licenses[0]
            assertEquals("ABC123456", license.code)
            assertEquals(123456, license.id)
            assertEquals("CENTRE EXCURSIONISTA D'ALCOI", license.club)
            assertEquals(5, license.modalityId)
            assertEquals("FEMECV 2025", license.modalityName)
            assertEquals(LocalDate(2025, 1, 1), license.validFrom)
            assertEquals(LocalDate(2025, 12, 31), license.validTo)
            assertEquals(11, license.categoryId)
            assertEquals(69, license.subCategoryId)
        } finally {
            FEMECV.resetEngine()
        }
    }
}
