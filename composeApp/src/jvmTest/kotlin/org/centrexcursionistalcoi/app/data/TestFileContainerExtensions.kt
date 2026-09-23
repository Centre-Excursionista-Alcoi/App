package org.centrexcursionistalcoi.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.io.files.Path
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.centrexcursionistalcoi.app.network._httpClient
import org.centrexcursionistalcoi.app.storage.fs.exists
import org.junit.AfterClass
import org.junit.BeforeClass
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Regression coverage for a real (if narrowly-avoided) bug: [DocumentFileContainer.fetchDocumentFilePath]/the
 * private `fetchImageFilePath` used to re-derive a [Uuid] from the very path string they'd just built from a real
 * [Uuid] a few lines above (`path.substringAfterLast(...).toUuidOrNull()`), instead of just using that [Uuid]
 * directly -- harmless only as long as the cached filename never carries anything but a bare UUID. A parallel
 * `FileContainer.fetchFilePath(uuid)` briefly *did* append a `.pdf` extension to cached filenames (reverted the
 * very next commit), which would have made that re-derivation always fail -- `fetchDocumentFilePath` would have
 * thrown "UUID could not be inferred" on every cache miss instead of downloading. These tests exercise the real
 * download decision (via the [_httpClient] mock seam [TestQualificationsRemoteRepository] also uses) so a
 * regression like that fails loudly instead of silently depending on filenames staying extension-free.
 */
class TestFileContainerExtensions {
    companion object {
        private val tempDir = File(System.getProperty("java.io.tmpdir"), "TestFileContainerExtensions-${UUID.randomUUID()}").apply { mkdirs() }

        @BeforeClass
        @JvmStatic
        fun setUpKoin() {
            startKoin {
                modules(
                    module {
                        single<PathsProvider> {
                            object : PathsProvider {
                                override val systemDataPath: Path get() = Path(tempDir.absolutePath)
                            }
                        }
                    }
                )
            }
        }

        @AfterClass
        @JvmStatic
        fun tearDownKoin() {
            stopKoin()
            tempDir.deleteRecursively()
        }
    }

    private val originalHttpClient = _httpClient
    private val requests = mutableListOf<HttpRequestData>()

    @AfterTest
    fun tearDown() {
        _httpClient = originalHttpClient
        tempDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun mockDownload(content: String = "file content", status: HttpStatusCode = HttpStatusCode.OK) {
        _httpClient = HttpClient(MockEngine { request ->
            requests += request
            respond(content, status)
        })
    }

    private fun insurance(documentId: Uuid?) = UserInsurance(
        id = Uuid.random(),
        userSub = "user-sub",
        insuranceCompany = "Insurance Co.",
        policyNumber = "POL123456789",
        validFrom = LocalDate(2024, 1, 1),
        validTo = LocalDate(2025, 1, 1),
        documentId = documentId,
    )

    @Test
    fun fetchDocumentFilePath_whenMissing_downloadsUsingTheContainersRealUuid() = runTest {
        val uuid = Uuid.random()
        mockDownload()

        val file = insurance(uuid).fetchDocumentFilePath()

        assertEquals(1, requests.size)
        assertEquals("/download/$uuid", requests.single().url.encodedPath)
        assertEquals("documents/UserInsurance/$uuid", file.relativePath)
        assertTrue(file.exists())
    }

    @Test
    fun fetchDocumentFilePath_downloadIfNotExists_false_skipsTheDownload() = runTest {
        val uuid = Uuid.random()
        mockDownload()

        val file = insurance(uuid).fetchDocumentFilePath(downloadIfNotExists = false)

        assertTrue(requests.isEmpty())
        assertEquals("documents/UserInsurance/$uuid", file.relativePath)
    }

    @Test
    fun fetchDocumentFilePath_whenAlreadyCached_doesNotRedownload() = runTest {
        val uuid = Uuid.random()
        mockDownload()
        val cachedDir = File(tempDir, "documents/UserInsurance").apply { mkdirs() }
        File(cachedDir, uuid.toString()).writeText("already cached")

        val file = insurance(uuid).fetchDocumentFilePath()

        assertTrue(requests.isEmpty())
        assertEquals("already cached", File(tempDir, file.relativePath).readText())
    }

    @Test
    fun fetchImageFilePath_whenMissing_downloadsUsingTheContainersRealUuid() = runTest {
        val uuid = Uuid.random()
        mockDownload()
        val department = Department(id = Uuid.random(), displayName = "Test Department", image = uuid, members = null)

        val file = department.fetchImageFilePath()

        assertEquals(1, requests.size)
        assertEquals("/download/$uuid", requests.single().url.encodedPath)
        assertEquals("images/Department/$uuid", file.relativePath)
        assertTrue(file.exists())
    }
}
