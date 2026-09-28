package org.centrexcursionistalcoi.app.storage

import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.database.Database
import org.centrexcursionistalcoi.app.database.entity.FileEntity
import org.centrexcursionistalcoi.app.security.FileReadWriteRules
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Gives every test a new, empty [InMemoryFileStorage] (see [testStorage]). Registered for all tests automatically
 * (`META-INF/services`). A test may replace it with [FileStorageProvider.useForTests].
 */
class TestFileStorageExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        FileStorageProvider.useForTests(InMemoryFileStorage())
    }

    override fun afterEach(context: ExtensionContext) {
        FileStorageProvider.useForTests(null)
    }
}

/**
 * The storage of the running test, see [TestFileStorageExtension].
 */
val testStorage: InMemoryFileStorage
    get() = FileStorageProvider.current as? InMemoryFileStorage
        ?: error("The test replaced the in-memory storage with ${FileStorageProvider.current.description}")

/**
 * Creates a file with the given contents, stored in the test's storage. Its values are loaded, so they can be read
 * outside a transaction.
 */
fun createTestFile(
    bytes: ByteArray,
    name: String? = "file",
    contentType: ContentType? = null,
    rules: FileReadWriteRules? = null,
    id: java.util.UUID? = null,
): FileEntity = Database { FileEntity.create(bytes, name, contentType, rules, id).apply { refresh(flush = true) } }
