package org.centrexcursionistalcoi.app.pdf

import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.centrexcursionistalcoi.app.data.ZonedDateTime
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.database.entity.MemoryEntity
import org.centrexcursionistalcoi.app.test.FakeUser
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock

/**
 * [PdfGeneratorService.updateMemoriesIfNeeded], which runs as the server starts.
 */
class TestUpdateMemoriesPdfs {
    @AfterTest
    fun tearDown() = runTest {
        Database.clear()
    }

    private fun newMemory(pdf: FileEntity? = null) = Database {
        MemoryEntity.new {
            text = "A **memory**"
            submittedBy = FakeUser.provideEntity()
            from = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            to = ZonedDateTime.fromInstant(Clock.System.now(), TimeZone.currentSystemDefault())
            this.pdf = pdf
        }
    }

    @Test
    fun test_generatesMissingAndOutdatedPdfs() = runTest {
        Database.initForTests()
        val withoutPdf = newMemory()
        // Not a PDF this version generated
        val outdated = Database {
            FileEntity.create(bytes = "not a pdf".encodeToByteArray(), name = "old.pdf", contentType = ContentType.Application.Pdf)
        }
        val withOutdatedPdf = newMemory(outdated)

        PdfGeneratorService.updateMemoriesIfNeeded()

        Database {
            val generated = assertNotNull(MemoryEntity[withoutPdf.id].pdf, "A memory without PDF must get one")
            assertFalse(PdfGeneratorService.needsUpdate(generated))
            val replaced = assertNotNull(MemoryEntity[withOutdatedPdf.id].pdf)
            assertNotEquals(outdated.id, replaced.id, "The outdated PDF must be replaced")
            assertFalse(PdfGeneratorService.needsUpdate(replaced))
        }
    }

    @Test
    fun test_upToDatePdfsAreKept() = runTest {
        Database.initForTests()
        val memory = newMemory()
        PdfGeneratorService.updateMemoriesIfNeeded()
        val pdf = Database { MemoryEntity[memory.id].pdf!!.id }

        PdfGeneratorService.updateMemoriesIfNeeded()

        Database { assertEquals(pdf, MemoryEntity[memory.id].pdf!!.id) }
    }
}
