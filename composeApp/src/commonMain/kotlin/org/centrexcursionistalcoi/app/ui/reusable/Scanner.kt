package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Composable
fun Scanner(
    codeTypes: List<BarcodeFormat> = listOf(BarcodeFormat.FORMAT_ALL_FORMATS),
    onScan: (Barcode) -> Unit,
    onError: (Exception) -> Unit = {},
    onDismissRequest: () -> Unit
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = true),
        onDismissRequest = onDismissRequest
    ) {
        ScannerView(
            modifier = Modifier.fillMaxSize(),
            codeTypes = codeTypes
        ) { result ->
            when (result) {
                is BarcodeResult.OnSuccess -> {
                    onScan(result.barcode)
                    onDismissRequest()
                }

                is BarcodeResult.OnFailed -> onError(result.exception)

                BarcodeResult.OnCanceled -> onDismissRequest()
            }
        }
    }
}
