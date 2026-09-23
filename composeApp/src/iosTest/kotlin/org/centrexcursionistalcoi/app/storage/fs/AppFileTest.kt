@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@file:Suppress("CAST_NEVER_SUCCEEDS") // Kotlin/Native bridges String and NSString at runtime.

package org.centrexcursionistalcoi.app.storage.fs

import com.mohamedrejeb.calf.core.PlatformContext
import com.mohamedrejeb.calf.io.exists
import com.mohamedrejeb.calf.io.readByteArray
import io.ktor.http.ContentType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import org.centrexcursionistalcoi.app.di.IosPathsProvider
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataUsingEncoding
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Runs on a real simulator/device: exercises a real file under [IosPathsProvider.systemDataPath], the same way
 * [org.centrexcursionistalcoi.app.auth.CredentialsStoreTest] exercises the real Keychain.
 *
 * The whole reason [AppFile.toKmpFile] goes through [AppFile.toNSURL] here -- rather than building an `NSURL`
 * by hand at every call site -- is that the [com.mohamedrejeb.calf.io.KmpFile] it returns must actually be
 * usable: readable through Calf's own `exists()`/`readByteArray()`, not just structurally point at the right
 * path string.
 */
class AppFileTest {
    private val pathsProvider = IosPathsProvider()

    private val appFile = AppFile("AppFileTest-${Uuid.random()}.pdf")
    private val content = "fake pdf content"

    @BeforeTest
    fun setUp() {
        startKoin {
            modules(module { single<PathsProvider> { pathsProvider } })
        }

        val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)!!
        data.writeToFile(appFile.absolutePath.toString(), true)
    }

    @AfterTest
    fun tearDown() {
        NSFileManager.defaultManager.removeItemAtPath(appFile.absolutePath.toString(), null)
        stopKoin()
    }

    @Test
    fun toKmpFile_pointsAtTheRealFile() {
        val kmpFile = appFile.toKmpFile(ContentType.Application.Pdf)

        assertTrue(kmpFile.exists(PlatformContext.INSTANCE))
    }

    @Test
    fun toKmpFile_contentIsReadableThroughIt() = runTest {
        val kmpFile = appFile.toKmpFile(ContentType.Application.Pdf)

        val bytes = kmpFile.readByteArray(PlatformContext.INSTANCE)
        assertEquals(content, bytes.decodeToString())
    }

    @Test
    fun toKmpFile_aPathThatDoesNotExist_stillConstructs_butDoesNotExist() {
        val kmpFile = AppFile("does-not-exist-${Uuid.random()}.pdf").toKmpFile(ContentType.Application.Pdf)

        assertFalse(kmpFile.exists(PlatformContext.INSTANCE))
    }
}
