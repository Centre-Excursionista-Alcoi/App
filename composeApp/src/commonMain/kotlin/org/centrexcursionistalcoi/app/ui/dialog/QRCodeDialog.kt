package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import com.diamondedge.logging.logging
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.oned.BarcodeType
import io.github.alexzhirkevich.qrose.oned.rememberBarcodePainter
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.NfcPayload
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.platform.PlatformDragAndDrop
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.PlatformPrinter
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Nfc
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Print
import org.centrexcursionistalcoi.app.ui.reusable.buttons.DeleteButton
import org.centrexcursionistalcoi.app.ui.utils.modIf
import org.jetbrains.compose.resources.stringResource

private val Code39Regex = Regex("^[0-9A-Z \\-.$/+%]+$")
private val Code128Regex = Regex("^[\\x00-\\x7F]+$")

private val log = logging()

@Composable
fun QRCodeDialog(
    value: String,
    onReadNfc: (NfcPayload) -> Unit,
    onDeleteRequest: (() -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    var writingNFC by remember { mutableStateOf(false) }
    var readingNFC by remember { mutableStateOf(false) }
    if (writingNFC || readingNFC) {
        var job by remember { mutableStateOf<Job?>(null) }
        LaunchedEffect(Unit) {
            job = CoroutineScope(defaultAsyncDispatcher).launch {
                if (writingNFC) {
                    PlatformNFC.writeNFC(value)
                    writingNFC = false
                } else {
                    val nfcValue = PlatformNFC.readNFC()
                    if (nfcValue != null) {
                        log.d { "Read NFC: $nfcValue" }
                        onReadNfc(nfcValue)
                    }
                    readingNFC = false
                }
            }
        }

        AlertDialog(
            onDismissRequest = {
                job?.cancel()
                writingNFC = false
                readingNFC = false
            },
            title = { Text("Waiting for NFC tag") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MaterialSymbols.Nfc, null, modifier = Modifier.size(64.dp).padding(16.dp))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        job?.cancel()
                        writingNFC = false
                        readingNFC = false
                    }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("QR Code") },
        text = {
            Column {
                val qrCodePainter = rememberQrCodePainter(value)
                ImageDisplay(
                    qrCodePainter,
                    "QR Code",
                    imageModifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .modIf(PlatformDragAndDrop.isSupported) { dragAndDropSource { _ -> PlatformDragAndDrop.qrImageTransferData(qrCodePainter) } }
                )

                val barcodePainter = if (value.matches(Code39Regex)) {
                    rememberBarcodePainter(value, BarcodeType.Code39)
                } else if (value.matches(Code128Regex)) {
                    rememberBarcodePainter(value, BarcodeType.Code128)
                } else {
                    null
                }
                if (barcodePainter != null) {
                    ImageDisplay(barcodePainter, "Bar Code", imageModifier = Modifier.fillMaxWidth())
                }

                if (PlatformNFC.isSupported) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { writingNFC = true },
                        ) { Text("Write NFC") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { readingNFC = true },
                        ) { Text("Store NFC") }
                    }
                }
            }
        },
        dismissButton = if (onDeleteRequest != null) {
            { DeleteButton { onDeleteRequest() } }
        } else {
            null
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Composable
private fun ImageDisplay(
    painter: Painter,
    contentDescription: String? = null,
    imageModifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter,
            contentDescription,
            modifier = imageModifier.padding(top = 8.dp).padding(horizontal = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (PlatformPrinter.isSupported) {
                IconButton(
                    onClick = {
                        val imageData = painter.toByteArray(1024, 1024, ImageFormat.PNG)
                        PlatformPrinter.printImage(imageData)
                    }
                ) {
                    Icon(MaterialSymbols.Print, null)
                }
            }
            // TODO: Implement sharing QR codes and barcodes
            /*if (PlatformShareLogic.sharingSupported) {
                IconButton(
                    onClick = {
                        val imageData = painter.toByteArray(1024, 1024, ImageFormat.PNG)
                        PlatformShareLogic.share(imageData, ContentType.Image.PNG)
                    }
                ) {
                    Icon(Icons.Default.Share, null)
                }
            }*/
        }
    }
}
