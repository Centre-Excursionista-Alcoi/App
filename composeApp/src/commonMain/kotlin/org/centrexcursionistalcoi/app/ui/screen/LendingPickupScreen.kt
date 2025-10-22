package org.centrexcursionistalcoi.app.ui.screen

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
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FirstPage
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
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.setClipEntry
import org.centrexcursionistalcoi.app.ui.icons.BrandIcons
import org.centrexcursionistalcoi.app.ui.icons.Whatsapp
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LendingPickupViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import tech.kotlinlang.permission.HelperHolder
import tech.kotlinlang.permission.Permission
import tech.kotlinlang.permission.result.CameraPermissionResult

@Composable
fun LendingPickupScreen(
    lendingId: Uuid,
    model: LendingPickupViewModel = viewModel { LendingPickupViewModel(lendingId) },
    onBack: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    val lending by model.lending.collectAsState()
    val scannedItems by model.scannedItems.collectAsState()
    val scanError by model.scanError.collectAsState()
    val scanSuccess by model.scanSuccess.collectAsState()

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

    LendingPickupScreen(
        snackbarHostState = snackbarHostState,
        lending = lending,
        scannedItems = scannedItems,
        onScanCode = model::onScan,
        onMarkItem = model::onScan,
        onUnMarkItem = model::unmark,
        onCompleteRequest = model::pickup,
        onCancelRequest = model::cancelLending,
        onBack = onBack
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingPickupScreen(
    snackbarHostState: SnackbarHostState,
    lending: ReferencedLending?,
    scannedItems: Set<Uuid>,
    onScanCode: (Barcode) -> Unit,
    onMarkItem: (Uuid) -> Unit,
    onUnMarkItem: (Uuid) -> Unit,
    onCompleteRequest: () -> Job,
    onCancelRequest: () -> Job,
    onBack: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                title = { Text(stringResource(Res.string.management_pickup_screen)) },
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
                    LendingFAB(areAllItemsScanned, onCompleteRequest, onBack)
                }
            }
        },
    ) { paddingValues ->
        if (lending == null) LoadingBox(paddingValues)
        else Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            LendingPickupContent(lending, scannedItems, snackbarHostState, onMarkItem, onUnMarkItem)

            Spacer(Modifier.height(96.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun AnimatedVisibilityScope.LendingFAB(
    allItemsScanned: Boolean,
    onCompleteRequest: () -> Job,
    onBack: () -> Unit,
) {
    var showingSkipWarning by remember { mutableStateOf(false) }
    if (showingSkipWarning) {
        var isLoading by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isLoading) showingSkipWarning = false },
            title = { Text(stringResource(Res.string.management_pickup_screen_skip_warning_title)) },
            text = { Text(stringResource(Res.string.management_pickup_screen_skip_warning_message)) },
            confirmButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = {
                        onCompleteRequest().invokeOnCompletion {
                            isLoading = false
                            showingSkipWarning = false
                            onBack()
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
                        onBack()
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
    snackbarHostState: SnackbarHostState,
    onMarkItem: (Uuid) -> Unit,
    onUnMarkItem: (Uuid) -> Unit,
    supportsNFC: Boolean = PlatformNFC.supportsNFC,
) {
    val lendingUser = lending.user.lendingUser
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
                text = lendingUser.fullName,
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

    if (supportsNFC) {
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
            ScanItemListItem(
                text = type.displayName,
                scanned = allItemsScanned,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                onClickIcon = null,
            )
            for (item in items) {
                val isScanned = item.id in scannedItems
                ScanItemListItem(
                    text = item.id.toString(),
                    scanned = isScanned,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    if (isScanned) {
                        onUnMarkItem(item.id)
                    } else {
                        onMarkItem(item.id)
                    }
                }
            }
            if (index < groupedItems.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun ScanItemListItem(text: String, scanned: Boolean, style: TextStyle, modifier: Modifier = Modifier, onClickIcon: (() -> Unit)?) {
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
            } else {
                Icons.Default.RemoveCircleOutline
            },
            contentDescription = null,
            tint = if (scanned) {
                Color(0xFF4CAF50) // Green for scanned
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
