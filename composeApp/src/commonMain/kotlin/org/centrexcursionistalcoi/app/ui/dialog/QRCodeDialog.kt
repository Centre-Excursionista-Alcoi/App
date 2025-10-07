package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.close
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.oned.BarcodeType
import io.github.alexzhirkevich.qrose.oned.rememberBarcodePainter
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import io.ktor.http.ContentType
import org.centrexcursionistalcoi.app.platform.PlatformPrinter
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.jetbrains.compose.resources.stringResource

private val Code39Regex = Regex("^[0-9A-Z \\-.$/+%]+$")
private val Code128Regex = Regex("^[\\x00-\\x7F]+$")

@Composable
fun QRCodeDialog(value: String, onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("QR Code") },
        text = {
            Column {
                val qrCodePainter = rememberQrCodePainter(value)
                ImageDisplay(qrCodePainter, "QR Code")

                val barcodePainter = if (value.matches(Code39Regex)) {
                    rememberBarcodePainter(value, BarcodeType.Code39)
                } else if (value.matches(Code128Regex)) {
                    rememberBarcodePainter(value, BarcodeType.Code128)
                } else {
                    null
                }
                if (barcodePainter != null) {
                    ImageDisplay(barcodePainter, "Bar Code")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Composable
private fun ImageDisplay(painter: Painter, contentDescription: String? = null) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter,
            contentDescription,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(top = 8.dp).padding(horizontal = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (PlatformPrinter.supportsPrinting) {
                IconButton(
                    onClick = {
                        val imageData = painter.toByteArray(1024, 1024, ImageFormat.PNG)
                        PlatformPrinter.printImage(imageData)
                    }
                ) {
                    Icon(Icons.Default.Print, null)
                }
            }
            if (PlatformShareLogic.sharingSupported) {
                IconButton(
                    onClick = {
                        val imageData = painter.toByteArray(1024, 1024, ImageFormat.PNG)
                        PlatformShareLogic.share(imageData, ContentType.Image.PNG)
                    }
                ) {
                    Icon(Icons.Default.Share, null)
                }
            }
        }
    }
}
