package org.centrexcursionistalcoi.app.platform

import android.content.ClipData
import android.content.Context
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import io.ktor.http.*
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.centrexcursionistalcoi.app.storage.fs.contentUri
import org.centrexcursionistalcoi.app.storage.fs.toJavaFile
import org.koin.core.annotation.Singleton

@Singleton
actual class PlatformDragAndDrop(
    private val context: Context,
) : PlatformProvider {
    actual override val isSupported: Boolean = true

    actual fun imageTransferData(file: AppFile, contentType: ContentType): DragAndDropTransferData {
        // Get a content URI using FileProvider, copying under a name with the proper extension only if needed
        val uri = file.contentUri(context, contentType)

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
        val appFile = AppFile("qr/$name.$extension")
        val file = appFile.toJavaFile().apply {
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
        val uri = appFile.contentUri(context, contentType)

        return DragAndDropTransferData(
            ClipData.newUri(context.contentResolver, value, uri)
        )
    }
}
