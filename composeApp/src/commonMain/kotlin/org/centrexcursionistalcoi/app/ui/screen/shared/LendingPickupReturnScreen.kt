package org.centrexcursionistalcoi.app.ui.screen.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.FreeCancellation
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleStartEffect
import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.Permission
import org.centrexcursionistalcoi.app.permission.result.CameraPermissionResult
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.setClipEntry
import org.centrexcursionistalcoi.app.ui.icons.BrandIcons
import org.centrexcursionistalcoi.app.ui.icons.Whatsapp
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LendingPickupReturnViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Composable
fun LendingPickupReturnScreen(
    title: String,
    skipDialogTitle: String,
    skipDialogMessage: String,
    model: LendingPickupReturnViewModel,
    onBack: () -> Unit,
    onCompleteRequest: () -> Job,
    isItemToggleable: (Uuid) -> Boolean = { true },
) {
    val hapticFeedback = LocalHapticFeedback.current

    val lending by model.lending.collectAsState()
    val scannedItems by model.scannedItems.collectAsState()
    val dismissedItems by model.dismissedItems.collectAsState()
    val scanError by model.scanError.collectAsState()
    val scanSuccess by model.scanSuccess.collectAsState()
    val error by model.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleStartEffect(Unit) {
        model.startNfc()
        onStopOrDispose {
            model.stopNfc()
        }
    }

    LaunchedEffect(scanError) {
        val error = scanError
        if (error != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
            snackbarHostState.showSnackbar(error)
            model.clearScanResult()
        }
    }
    LaunchedEffect(scanSuccess) {
        if (scanSuccess != null) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
            model.clearScanResult()
        }
    }
    LaunchedEffect(error) {
        val error = error
        if (error != null) {
            snackbarHostState.showSnackbar(error.message ?: error::class.simpleName ?: "Unknown error")
            model.clearScanResult()
        }
    }

    LendingPickupReturnScreen(
        title = title,
        skipDialogTitle = skipDialogTitle,
        skipDialogMessage = skipDialogMessage,
        snackbarHostState = snackbarHostState,
        lending = lending,
        scannedItems = scannedItems,
        dismissedItems = dismissedItems,
        onScanCode = model::onScan,
        onToggleItem = model::toggleItem,
        onCompleteRequest = onCompleteRequest,
        onDeleteRequest = model::deleteLending,
        onBack = onBack,
        isItemToggleable = isItemToggleable,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingPickupReturnScreen(
    title: String,
    skipDialogTitle: String,
    skipDialogMessage: String,
    snackbarHostState: SnackbarHostState,
    lending: ReferencedLending?,
    scannedItems: Set<Uuid>,
    dismissedItems: Set<Uuid>,
    onScanCode: (Barcode) -> Unit,
    onToggleItem: (Uuid) -> Unit,
    onCompleteRequest: () -> Job,
    onDeleteRequest: () -> Job,
    onBack: () -> Unit,
    isItemToggleable: (Uuid) -> Boolean = { true },
) {
    val scope = rememberCoroutineScope()

    val permissionHelper = HelperHolder.getPermissionHelperInstance()

    var showingScanner by remember { mutableStateOf(false) }
    if (showingScanner) {
        ScannerView(
            modifier = Modifier.zIndex(2f),
            codeTypes = listOf(
                BarcodeFormat.FORMAT_QR_CODE,
                BarcodeFormat.FORMAT_CODE_39,
                BarcodeFormat.FORMAT_CODE_128,
            )
        ) { result ->
            when (result) {
                is BarcodeResult.OnSuccess -> {
                    val data = result.barcode.data
                    scope.launch { snackbarHostState.showSnackbar(getString(Res.string.scanner_read, data)) }
                    Napier.i { "Barcode: ${result.barcode.data}, format: ${result.barcode.format}" }
                    onScanCode(result.barcode)
                    showingScanner = false
                }

                is BarcodeResult.OnFailed -> {
                    Napier.e(result.exception) { "Could not read barcode." }
                    scope.launch { snackbarHostState.showSnackbar(getString(Res.string.scanner_error)) }
                    showingScanner = false
                }

                BarcodeResult.OnCanceled -> {
                    Napier.d { "Scan cancelled" }
                    showingScanner = false
                }
            }
        }
    }

    var showingCancelConfirmation by remember { mutableStateOf(false) }
    if (showingCancelConfirmation) {
        var isLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLoading) showingCancelConfirmation = false },
            title = { Text(stringResource(Res.string.lending_details_cancel_confirm_title)) },
            text = { Text(stringResource(Res.string.lending_details_cancel_confirm_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        isLoading = true
                        onDeleteRequest().invokeOnCompletion {
                            isLoading = false
                            showingCancelConfirmation = false
                            onBack()
                        }
                    }
                ) {
                    Text(stringResource(Res.string.lending_details_cancel))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = { showingCancelConfirmation = false }
                ) {
                    Text(stringResource(Res.string.close))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                title = { Text(title) },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch(defaultAsyncDispatcher) {
                                when (permissionHelper.checkIsPermissionGranted(Permission.Camera)) {
                                    CameraPermissionResult.Denied -> permissionHelper.requestForPermission(Permission.Camera)
                                    CameraPermissionResult.NotAllowed -> permissionHelper.openSettings()
                                    CameraPermissionResult.Granted -> showingScanner = true
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null)
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Left),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip { Text(stringResource(Res.string.lending_details_cancel)) }
                        },
                    ) {
                        IconButton(
                            onClick = { showingCancelConfirmation = true },
                        ) {
                            Icon(Icons.Default.FreeCancellation, stringResource(Res.string.lending_details_cancel))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (lending != null) {
                val allItemsScanned = lending.items.all { it.id in scannedItems }
                AnimatedContent(
                    targetState = allItemsScanned
                ) { areAllItemsScanned ->
                    LendingFAB(skipDialogTitle, skipDialogMessage, areAllItemsScanned, onCompleteRequest)
                }
            }
        },
    ) { paddingValues ->
        if (lending == null) LoadingBox(paddingValues)
        else Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            LendingPickupContent(lending, scannedItems, dismissedItems, snackbarHostState, onToggleItem, isItemToggleable)

            Spacer(Modifier.height(96.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun AnimatedVisibilityScope.LendingFAB(
    dialogTitle: String,
    dialogMessage: String,
    allItemsScanned: Boolean,
    onCompleteRequest: () -> Job,
) {
    var showingSkipWarning by remember { mutableStateOf(false) }
    if (showingSkipWarning) {
        var isLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLoading) showingSkipWarning = false },
            title = { Text(dialogTitle) },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        onCompleteRequest().invokeOnCompletion {
                            isLoading = false
                            showingSkipWarning = false
                        }
                    }
                ) { Text(stringResource(Res.string.management_pickup_screen_confirm)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = { showingSkipWarning = false }
                ) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    var isLoading by remember { mutableStateOf(false) }
    SharedTransitionLayout {
        if (isLoading) {
            FloatingActionButton(
                modifier = Modifier.sharedBounds(rememberSharedContentState("fab"), this@LendingFAB),
                onClick = { /* Nothing */ }
            ) {
                CircularProgressIndicator(
                    Modifier.sharedBounds(rememberSharedContentState("icon"), this@LendingFAB),
                )
            }
        } else if (allItemsScanned) {
            ExtendedFloatingActionButton(
                modifier = Modifier.sharedBounds(rememberSharedContentState("fab"), this@LendingFAB),
                onClick = {
                    isLoading = true
                    onCompleteRequest().invokeOnCompletion {
                        isLoading = false
                    }
                }
            ) {
                Icon(
                    Icons.Default.Check,
                    stringResource(Res.string.management_pickup_screen_confirm),
                    Modifier.sharedBounds(rememberSharedContentState("icon"), this@LendingFAB),
                )
                Text(
                    text = stringResource(Res.string.management_pickup_screen_confirm),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        } else {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Left),
                state = rememberTooltipState(),
                tooltip = {
                    PlainTooltip { Text(stringResource(Res.string.management_pickup_screen_skip)) }
                },
            ) {
                FloatingActionButton(
                    modifier = Modifier.sharedBounds(rememberSharedContentState("fab"), this@LendingFAB),
                    onClick = { showingSkipWarning = true }
                ) {
                    Icon(
                        Icons.Default.KeyboardDoubleArrowRight,
                        stringResource(Res.string.management_pickup_screen_skip),
                        Modifier.sharedBounds(rememberSharedContentState("icon"), this@LendingFAB),
                    )
                }
            }
        }
    }
}

@Composable
private fun LendingPickupContent(
    lending: ReferencedLending,
    scannedItems: Set<Uuid>,
    dismissedItems: Set<Uuid>,
    snackbarHostState: SnackbarHostState,
    onToggleItem: (Uuid) -> Unit,
    isItemToggleable: (Uuid) -> Boolean = { true },
) {
    val user = lending.user
    val lendingUser = user.lendingUser
    val items = lending.items

    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current

    if (lendingUser == null) {
        // TODO: Show error message when lending user is null
        // This situation should not be possible. Users must be signed up for creating lendings.
    } else {
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(Res.string.lending_details_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(12.dp)
            )
            DataRow(
                icon = Icons.Default.Numbers,
                titleRes = Res.string.lending_details_id,
                text = lending.id.toString(),
            )

            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp)) {
                Icon(Icons.Default.FirstPage, null, Modifier.padding(end = 8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.lending_details_from),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = lending.from.toString(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Icon(Icons.AutoMirrored.Default.LastPage, null, Modifier.padding(end = 8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.lending_details_until),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = lending.from.toString(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            lending.notes?.let { notes ->
                DataRow(
                    icon = Icons.AutoMirrored.Default.Notes,
                    titleRes = Res.string.lending_details_notes,
                    text = notes,
                )
            }

            DataRow(
                icon = Icons.Default.Face,
                titleRes = Res.string.lending_details_user,
                text = user.fullName,
            )

            Row(modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(lendingUser.phoneNumber)
                            snackbarHostState.showSnackbar(getString(Res.string.copied_to_clipboard))
                        }
                    }
                ) {
                    Icon(Icons.Default.ContactPhone, stringResource(Res.string.lending_details_copy_number))
                    Text(stringResource(Res.string.lending_details_copy_number), Modifier.weight(1f).padding(start = 8.dp))
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                    onClick = {
                        uriHandler.openUri("mailto:${lending.user.email}")
                    }
                ) {
                    Icon(Icons.Default.Email, stringResource(Res.string.lending_details_email))
                    Text(stringResource(Res.string.lending_details_email), Modifier.weight(1f).padding(start = 8.dp))
                }
            }

            Row(modifier = Modifier.padding(bottom = 12.dp, start = 12.dp, end = 12.dp)) {
                val phone = lendingUser.phoneNumber
                // In Spain, mobile phones never start with 8 or 9
                val isMobilePhone = phone.removePrefix("+34").trim().let { !it.startsWith("8") || !it.startsWith("9") }
                val internationalPhone = if (phone.startsWith('+')) {
                    phone
                } else {
                    "+34$phone"
                }.replace(" ", "").replace("(", "").replace(")", "")

                OutlinedButton(
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    onClick = {
                        uriHandler.openUri("tel:$internationalPhone")
                    }
                ) {
                    Icon(Icons.Default.Call, stringResource(Res.string.lending_details_call_user))
                    Text(stringResource(Res.string.lending_details_call_user), Modifier.weight(1f).padding(start = 8.dp))
                }
                if (isMobilePhone) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                        onClick = {
                            uriHandler.openUri("https://wa.me/$internationalPhone")
                        }
                    ) {
                        Icon(BrandIcons.Whatsapp, stringResource(Res.string.lending_details_whatsapp))
                        Text(stringResource(Res.string.lending_details_whatsapp), Modifier.weight(1f).padding(start = 8.dp))
                    }
                }
            }
        }
    }

    if (PlatformNFC.isSupported) {
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Icon(
                    Icons.Default.Nfc,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 16.dp, end = 16.dp, start = 4.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.lending_details_nfc_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(Res.string.lending_details_nfc_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    // TODO: Info box

    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val groupedItems = items.groupBy { it.type }
        for ((index, entry) in groupedItems.entries.withIndex()) {
            val (type, items) = entry
            val allItemsScanned = items.all { it.id in scannedItems }
            val allItemsDismissed = items.all { it.id in dismissedItems }
            ScanItemListItem(
                text = type.displayName,
                scanned = allItemsScanned,
                dismissed = allItemsDismissed,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClickIcon = null,
            )
            for (item in items) {
                val isScanned = item.id in scannedItems
                val isDismissed = item.id in dismissedItems
                ScanItemListItem(
                    text = item.id.toString(),
                    scanned = isScanned,
                    dismissed = isDismissed,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClickIcon = if (isItemToggleable(item.id)) {
                        { onToggleItem(item.id) }
                    } else null,
                )
            }
            if (index < groupedItems.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun ScanItemListItem(
    text: String,
    scanned: Boolean,
    dismissed: Boolean,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onClickIcon: (() -> Unit)?
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = style,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .padding(end = 8.dp, start = 12.dp)
        )
        Icon(
            imageVector = if (scanned) {
                Icons.Default.CheckCircleOutline
            } else if (dismissed) {
                Icons.Default.RemoveCircleOutline
            } else {
                Icons.AutoMirrored.Default.HelpOutline
            },
            contentDescription = null,
            tint = if (scanned) {
                Color(0xFF4CAF50) // Green for scanned
            } else if (dismissed) {
                Color(0xffcd1919) // Red for dismissed
            } else {
                Color(0xFF368CF4) // Blue for not scanned
            },
            modifier = Modifier.padding(8.dp).clickable(enabled = onClickIcon != null) { onClickIcon?.invoke() },
        )
    }
}

@Composable
private fun DataRow(
    titleRes: StringResource,
    text: String,
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp)
    ) {
        Icon(icon, contentDescription, Modifier.padding(end = 8.dp))
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
