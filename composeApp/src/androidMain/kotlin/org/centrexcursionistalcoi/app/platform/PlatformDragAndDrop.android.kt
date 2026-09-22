package org.centrexcursionistalcoi.app.platform

import android.content.ClipData
import android.content.Context
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import io.github.vinceglb.filekit.utils.div
import io.ktor.http.*
import org.centrexcursionistalcoi.app.di.PathsProvider
import org.centrexcursionistalcoi.app.storage.fs.FilePermissionsUtil
import org.koin.core.annotation.Singleton
import java.io.File

@Singleton
actual class PlatformDragAndDrop(
    private val context: Context,
    private val pathsProvider: PathsProvider,
) : PlatformProvider {
    actual override val isSupported: Boolean = true

    actual fun imageTransferData(path: String, contentType: ContentType): DragAndDropTransferData {
        // Hard-link the data under a name with the proper extension and get a content URI using FileProvider
        val filePath = pathsProvider.systemDataPath / path
        val file = File(filePath.toString())
        val uri = FilePermissionsUtil.uriForFile(context, file, contentType)

        return DragAndDropTransferData(
            ClipData.newUri(context.contentResolver, "", uri)
        )
    }

    actual fun qrImageTransferData(
        painter: QrCodePainter,
        value: String,
        contentType: ContentType
    ): DragAndDropTransferData {
        val extension = contentType.fileExtensions().first()
        val name = value.hashCode()
        val filePath = pathsProvider.systemDataPath / "qr" / "$name.$extension"
        val file = File(filePath.toString()).apply {
            parentFile?.mkdirs()
            if (!exists()) {
                outputStream().use { output ->
                    output.write(
                        painter.toByteArray(
                            512, 512, when (contentType) {
                                ContentType.Image.PNG -> ImageFormat.PNG
                                ContentType.Image.JPEG -> ImageFormat.JPEG
                                ContentType.Image.WEBP -> ImageFormat.WEBP
                                else -> error("Unsupported image format: $contentType")
                            }
                        )
                    )
                }
            }
        }
        val uri = FilePermissionsUtil.uriForFile(context, file, contentType)

        return DragAndDropTransferData(
            ClipData.newUri(context.contentResolver, value, uri)
        )
    }
}
