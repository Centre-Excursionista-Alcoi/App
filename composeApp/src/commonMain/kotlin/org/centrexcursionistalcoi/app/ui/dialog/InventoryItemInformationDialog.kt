package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import com.diamondedge.logging.logging
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.NfcPayload
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem
import org.centrexcursionistalcoi.app.data.manufacturer.ManufacturerItemDetails
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.platform.PlatformDragAndDrop
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.PlatformPrinter
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Nfc
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Print
import org.centrexcursionistalcoi.app.ui.reusable.Scanner
import org.centrexcursionistalcoi.app.ui.reusable.buttons.DeleteButton
import org.centrexcursionistalcoi.app.ui.utils.modIf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val log = logging()

@Composable
fun InventoryItemInformationDialog(
    item: ReferencedInventoryItem,
    onReadNfc: (NfcPayload) -> Unit,
    onReadManufacturerData: (String) -> Unit,
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
                    PlatformNFC.writeNFC(item.id.toString())
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
            title = { Text(stringResource(Res.string.nfc_waiting)) },
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

    var showingScanner by remember { mutableStateOf(false) }
    if (showingScanner) {
        Scanner(
            onScan = { barcode ->
                val data = barcode.data
                log.i { "Barcode: ${barcode.data}, format: ${barcode.format}" }
                onReadManufacturerData(data)
            },
            onDismissRequest = {
                showingScanner = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.inventory_item_type_title)) },
        text = {
            Column {
                val qrCodePainter = rememberQrCodePainter(item.id.toString())
                ImageDisplay(
                    qrCodePainter,
                    stringResource(Res.string.qrcode),
                    imageModifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .modIf(PlatformDragAndDrop.isSupported) {
                            dragAndDropSource { _ ->
                                PlatformDragAndDrop.qrImageTransferData(
                                    qrCodePainter
                                )
                            }
                        }
                )

                if (PlatformNFC.isSupported) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { writingNFC = true },
                    ) { Text(stringResource(Res.string.nfc_write)) }
                    Text(
                        text = stringResource(Res.string.nfc_write_help),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { readingNFC = true },
                    ) { Text(stringResource(Res.string.nfc_store)) }
                    Text(
                        text = stringResource(Res.string.nfc_store_help),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val manufacturerItemDetails = remember(item) {
                    item.manufacturerTraceabilityCode?.let(ManufacturerItemDetails::decode)
                }
                if (manufacturerItemDetails != null) {
                    OutlinedCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(manufacturerItemDetails.logo),
                                contentDescription = manufacturerItemDetails.name,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = stringResource(
                                    Res.string.inventory_item_manufacturer_details,
                                    manufacturerItemDetails.name
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showingScanner = true },
                    ) { Text(stringResource(Res.string.inventory_item_set_manufacturer_details)) }
                    Text(
                        text = stringResource(Res.string.inventory_item_set_manufacturer_details_help),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.fillMaxWidth()
                    )
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
            modifier = imageModifier.padding(8.dp),
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
