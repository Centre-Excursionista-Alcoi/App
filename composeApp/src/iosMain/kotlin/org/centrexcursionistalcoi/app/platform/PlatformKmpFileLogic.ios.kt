package org.centrexcursionistalcoi.app.platform

import com.mohamedrejeb.calf.io.KmpFile
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import kotlinx.cinterop.ExperimentalForeignApi
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.koin.core.annotation.Singleton
import platform.Foundation.NSURL

@Singleton
actual class PlatformKmpFileLogic(private val pathsProvider: PathsProvider) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun kmpFile(path: String, contentType: ContentType): KmpFile {
        val filePath = pathsProvider.systemDataPath / path
        val url = NSURL.fileURLWithPath(filePath.toString())
        return KmpFile(url)
    }
}
