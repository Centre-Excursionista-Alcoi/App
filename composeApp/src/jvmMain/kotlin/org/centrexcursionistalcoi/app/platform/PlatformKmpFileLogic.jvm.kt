package org.centrexcursionistalcoi.app.platform

import com.mohamedrejeb.calf.io.KmpFile
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.koin.core.annotation.Singleton
import java.io.File

@Singleton
actual class PlatformKmpFileLogic(
    private val pathsProvider: PathsProvider,
) {
    actual fun kmpFile(path: String, contentType: ContentType): KmpFile {
        val filePath = pathsProvider.systemDataPath / path
        val file = File(filePath.toString())
        return KmpFile(file)
    }
}
