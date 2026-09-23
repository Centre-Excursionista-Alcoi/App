@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@file:Suppress("CAST_NEVER_SUCCEEDS") // Kotlin/Native bridges String and NSString at runtime.

package org.centrexcursionistalcoi.app.platform

import com.mohamedrejeb.calf.core.PlatformContext
import com.mohamedrejeb.calf.io.exists
import com.mohamedrejeb.calf.io.readByteArray
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.di.IosPathsProvider
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.uuid.Uuid

/**
 * Runs on a real simulator/device: exercises a real file under [IosPathsProvider.systemDataPath], the same way
 * [org.centrexcursionistalcoi.app.auth.CredentialsStoreTest] exercises the real Keychain.
 *
 * The whole reason [PlatformKmpFileLogic] exists -- rather than building an `NSURL` by hand at every call site --
 * is that the [com.mohamedrejeb.calf.io.KmpFile] it returns must actually be usable: readable through Calf's own
 * `exists()`/`readByteArray()`, not just structurally point at the right path string.
 */
class PlatformKmpFileLogicTest {
    private val pathsProvider = IosPathsProvider()
    private val logic = PlatformKmpFileLogic(pathsProvider)

    private val relativePath = "PlatformKmpFileLogicTest-${Uuid.random()}.pdf"
    private val content = "fake pdf content"
    private val absolutePath get() = (pathsProvider.systemDataPath / relativePath).toString()

    @BeforeTest
    fun setUp() {
        val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
        data.writeToFile(absolutePath, true)
    }

    @AfterTest
    fun tearDown() {
        NSFileManager.defaultManager.removeItemAtPath(absolutePath, null)
    }

    @Test
    fun kmpFile_pointsAtTheRealFile() {
        val kmpFile = logic.kmpFile(relativePath, ContentType.Application.Pdf)

        assertTrue(kmpFile.exists(PlatformContext.INSTANCE))
    }

    @Test
    fun kmpFile_contentIsReadableThroughIt() = runTest {
        val kmpFile = logic.kmpFile(relativePath, ContentType.Application.Pdf)

        val bytes = kmpFile.readByteArray(PlatformContext.INSTANCE)
        assertEquals(content, bytes.decodeToString())
    }

    @Test
    fun kmpFile_aPathThatDoesNotExist_stillConstructs_butDoesNotExist() {
        val kmpFile = logic.kmpFile("does-not-exist-${Uuid.random()}.pdf", ContentType.Application.Pdf)

        assertFalse(kmpFile.exists(PlatformContext.INSTANCE))
    }
}
