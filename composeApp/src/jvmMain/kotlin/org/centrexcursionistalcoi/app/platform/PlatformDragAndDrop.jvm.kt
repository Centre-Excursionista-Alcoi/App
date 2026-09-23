package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.storage.fs.AppFile
import org.centrexcursionistalcoi.app.storage.fs.toJavaFile
import org.centrexcursionistalcoi.app.transfer.ByteArrayTransferable
import org.centrexcursionistalcoi.app.transfer.FileTransferable
import org.koin.core.annotation.Singleton

@Singleton
@OptIn(ExperimentalComposeUiApi::class)
actual class PlatformDragAndDrop : PlatformProvider {
    actual override val isSupported: Boolean = false // not working: throws unknown error

    actual fun imageTransferData(file: AppFile, contentType: ContentType): DragAndDropTransferData {
        return DragAndDropTransferData(
            DragAndDropTransferable(
                FileTransferable(file.toJavaFile()),
            ),
            supportedActions = listOf(DragAndDropTransferAction.Move)
        )
    }

    actual fun qrImageTransferData(
        painter: QrCodePainter,
        value: String,
        contentType: ContentType
    ): DragAndDropTransferData {
        return DragAndDropTransferData(
            DragAndDropTransferable(
                ByteArrayTransferable(painter.toByteArray(512, 512, ImageFormat.PNG)),
            ),
            supportedActions = listOf(DragAndDropTransferAction.Move),
        )
    }
}
