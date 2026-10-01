package org.centrexcursionistalcoi.app.routes

import io.ktor.client.plugins.resources.post
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink
import org.apache.pdfbox.text.PDFTextStripper
import org.centrexcursionistalcoi.app.ApplicationTestBase
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.request.CreateMemoryRequest
import org.centrexcursionistalcoi.app.routes.Api
import org.centrexcursionistalcoi.app.test.LoginType
import org.centrexcursionistalcoi.app.utils.requestWithFilesBody
import org.centrexcursionistalcoi.app.verification.DocumentType
import org.centrexcursionistalcoi.app.verification.DocumentVerification

class TestVerifyRoutes : ApplicationTestBase() {
    private val document = "The original document".encodeToByteArray()

    /** Records [document] as generated, returning its code. */
    private fun recordDocument(): String {
        val code = DocumentVerification.newCode()
        Database { DocumentVerification.record(code, document, DocumentType.MEMORY, Uuid.random()) }
        return code
    }

    private suspend fun ApplicationTestBuilder.postVerify(code: String?, file: ByteArray?): HttpResponse =
        client.post("/verify") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("code", code.orEmpty())
                        // What a browser sends when no file was chosen: an empty part
                        append("file", file ?: ByteArray(0), Headers.build {
                            append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"${if (file == null) "" else "document.pdf"}\"")
                        })
                    }
                )
            )
        }

    @Test
    fun test_get_withoutCode_showsTheForm() = runApplicationTest {
        val response = client.get("/verify")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), """name="code"""")
    }

    @Test
    fun test_get_code_showsTheDocument() = runApplicationTest {
        val code = recordDocument()

        // As someone may type it
        val response = client.get("/verify?code=${DocumentVerification.format(code).lowercase()}")

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.bodyAsText()
        assertContains(page, "This code belongs to a document")
        assertContains(page, DocumentVerification.format(code))
        assertContains(page, "Activity memory")
        assertContains(page, DocumentVerification.sha256(document))
    }

    @Test
    fun test_get_invalidCode_isEscaped() = runApplicationTest {
        val response = client.get("/verify?code=%3Cscript%3Ealert(1)%3C/script%3E")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val page = response.bodyAsText()
        assertContains(page, "&lt;script&gt;")
        assertFalse("<script>alert" in page)
    }

    @Test
    fun test_get_unknownCode_notFound() = runApplicationTest {
        val response = client.get("/verify?code=${DocumentVerification.newCode()}")

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(response.bodyAsText(), "No document has this verification code")
    }

    @Test
    fun test_post_codeAndItsFile_match() = runApplicationTest {
        val code = recordDocument()

        val response = postVerify(DocumentVerification.format(code), document)

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "The file is authentic")
    }

    @Test
    fun test_post_codeAndAnotherFile_mismatch() = runApplicationTest {
        val code = recordDocument()

        val response = postVerify(code, "The original document, changed".encodeToByteArray())

        assertEquals(HttpStatusCode.Conflict, response.status)
        val page = response.bodyAsText()
        assertContains(page, "it may have been changed")
        // Nothing about the document the code is for
        assertFalse(DocumentVerification.sha256(document) in page)
    }

    @Test
    fun test_post_codeOnly_showsTheDocument() = runApplicationTest {
        val code = recordDocument()

        val response = postVerify(code, null)

        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), "This code belongs to a document")
    }

    @Test
    fun test_post_fileOnly_findsItsDocument() = runApplicationTest {
        val code = recordDocument()

        val response = postVerify(null, document)

        assertEquals(HttpStatusCode.OK, response.status)
        val page = response.bodyAsText()
        assertContains(page, "The file is authentic")
        assertContains(page, DocumentVerification.format(code))
    }

    @Test
    fun test_post_unknownFileOnly_notFound() = runApplicationTest {
        recordDocument()

        val response = postVerify(null, "Some other file".encodeToByteArray())

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(response.bodyAsText(), "wasn't generated by the Centre Excursionista d'Alcoi")
    }

    @Test
    fun test_post_tooLargeFile() = runApplicationTest {
        val response = postVerify(null, ByteArray(21 * 1024 * 1024))

        assertEquals(HttpStatusCode.PayloadTooLarge, response.status)
        assertContains(response.bodyAsText(), "The file is too large")
    }

    @Test
    fun test_post_nothing_badRequest() = runApplicationTest {
        val response = postVerify(null, null)

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "Enter a verification code, or choose a PDF file")
    }

    @Test
    fun test_memoryPdf_isRecorded_andVerifies() = runApplicationTest(shouldLogIn = LoginType.USER) {
        val zone = TimeZone.currentSystemDefault()
        val request = CreateMemoryRequest(
            text = "A memory",
            from = ZonedDateTime(zone, LocalDate(2025, 6, 15), LocalTime(10, 0, 0)),
            to = ZonedDateTime(zone, LocalDate(2025, 6, 15), LocalTime(12, 0, 0)),
        )
        val location = client.post(Api.Memories()) {
            setBody(requestWithFilesBody(request, CreateMemoryRequest.serializer()))
        }.headers[HttpHeaders.Location]!!
        val memoryId = Uuid.parse(location.substringAfterLast('/'))
        val pdf = Database { MemoryEntity[memoryId].pdf!!.readBytes() }

        // Recorded with the PDF as stored
        val recorded = Database { DocumentVerification.findBySha256(DocumentVerification.sha256(pdf)) }.single()
        assertEquals(memoryId, recorded.subject)
        assertEquals(DocumentType.MEMORY, recorded.type)

        // Its code, and the link to verify it, on every page
        Loader.loadPDF(pdf).use { document ->
            val stripper = PDFTextStripper()
            for (page in 1..document.numberOfPages) {
                stripper.startPage = page
                stripper.endPage = page
                assertContains(stripper.getText(document), "Codi de verificació: ${recorded.code}")
            }
            val code = DocumentVerification.normalize(recorded.code)!!
            val link = document.getPage(0).annotations.filterIsInstance<PDAnnotationLink>().single()
            assertEquals(DocumentVerification.url(code), assertNotNull(link.action as? PDActionURI).uri)
        }

        // And it verifies
        val response = postVerify(null, pdf)
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), recorded.code)
    }
}
