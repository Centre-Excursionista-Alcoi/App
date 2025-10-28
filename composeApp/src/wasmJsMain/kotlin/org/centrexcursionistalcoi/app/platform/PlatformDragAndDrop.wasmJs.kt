package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.draganddrop.DragAndDropTransferData
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.ktor.http.ContentType
import org.w3c.dom.DataTransfer

// TODO: Most likely this won't work

@OptIn(ExperimentalWasmJsInterop::class)
val newDataTransfer: DataTransfer = js("new DataTransfer()")

actual fun imageTransferData(path: String, contentType: ContentType): DragAndDropTransferData {
    val transfer = newDataTransfer.apply {
        setData("text/uri-list", path)
    }

    return DragAndDropTransferData(transfer)
}

actual fun qrImageTransferData(painter: QrCodePainter, value: String, contentType: ContentType): DragAndDropTransferData {
    val transfer = newDataTransfer.apply {
        setData("text/uri-list", value)
    }

    return DragAndDropTransferData(transfer)
}
