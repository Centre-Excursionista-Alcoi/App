package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.draganddrop.DragAndDropTransferData
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.ktor.http.ContentType

actual object PlatformDragAndDrop : PlatformProvider {
    actual override val isSupported: Boolean = false

    actual fun imageTransferData(path: String, contentType: ContentType): DragAndDropTransferData {
        throw UnsupportedOperationException("Drag and Drop is not supported on iOS")
    }

    actual fun qrImageTransferData(
        painter: QrCodePainter,
        value: String,
        contentType: ContentType
    ): DragAndDropTransferData {
        throw UnsupportedOperationException("Drag and Drop is not supported on iOS")
    }
}
