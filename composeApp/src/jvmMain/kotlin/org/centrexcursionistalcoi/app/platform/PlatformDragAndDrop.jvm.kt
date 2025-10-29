package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropTransferAction
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.DragAndDropTransferable
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import io.ktor.http.ContentType
import java.io.File
import org.centrexcursionistalcoi.app.transfer.ByteArrayTransferable
import org.centrexcursionistalcoi.app.transfer.FileTransferable

@OptIn(ExperimentalComposeUiApi::class)
actual object PlatformDragAndDrop : PlatformProvider {
    actual override val isSupported: Boolean
        get() = TODO("Not yet implemented")

    actual fun imageTransferData(path: String, contentType: ContentType): DragAndDropTransferData {
        return DragAndDropTransferData(
            DragAndDropTransferable(
                FileTransferable(File(path)),
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
