package org.centrexcursionistalcoi.app.ui.reusable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.Permission
import org.centrexcursionistalcoi.app.permission.result.CameraPermissionResult
import org.jetbrains.compose.resources.stringResource
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
    val scope = rememberCoroutineScope()
    val permissionHelper = remember { HelperHolder.getPermissionHelperInstance() }
    var permissionResult by remember { mutableStateOf<CameraPermissionResult?>(null) }

    LifecycleResumeEffect(Unit) {
        scope.launch {
            permissionResult = permissionHelper.checkIsPermissionGranted(Permission.Camera)
        }
        onPauseOrDispose { /*nothing*/ }
    }

    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = true),
        onDismissRequest = onDismissRequest
    ) {
        if (permissionResult == CameraPermissionResult.Granted) {
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
        } else {
            OutlinedCard {
                Text(
                    text = stringResource(Res.string.permission_camera_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.permission_camera_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (permissionResult == CameraPermissionResult.Denied) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                permissionResult = permissionHelper.requestForPermission(Permission.Camera)
                            }
                        }
                    ) { Text(stringResource(Res.string.permission_grant)) }
                } else {
                    OutlinedButton(
                        onClick = {
                            permissionHelper.openSettings()
                        }
                    ) { Text(stringResource(Res.string.permission_settings)) }
                }
            }
        }
    }
}
