package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.draganddrop.DragAndDropTransferData
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.ktor.http.ContentType

expect object PlatformDragAndDrop : PlatformProvider {
    override val isSupported: Boolean

    fun imageTransferData(path: String, contentType: ContentType): DragAndDropTransferData

    fun qrImageTransferData(painter: QrCodePainter, value: String = painter.data, contentType: ContentType = ContentType.Image.PNG): DragAndDropTransferData
}
