package org.centrexcursionistalcoi.app.ui.screen.admin

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.diamondedge.logging.logging
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.centrexcursionistalcoi.app.permission.HelperHolder
import org.centrexcursionistalcoi.app.permission.Permission
import org.centrexcursionistalcoi.app.permission.result.CameraPermissionResult
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.platform.setClipEntry
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.icons.BrandIcons
import org.centrexcursionistalcoi.app.ui.icons.Whatsapp
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.screen.DataRow
import org.centrexcursionistalcoi.app.ui.screen.GeneralLendingDetails
import org.centrexcursionistalcoi.app.ui.screen.LendingItems
import org.centrexcursionistalcoi.app.ui.screen.MemoryViewButtons
import org.centrexcursionistalcoi.app.utils.withoutSeconds
import org.centrexcursionistalcoi.app.viewmodel.LendingManagementViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView
import kotlin.uuid.Uuid

private val log = logging()

@Composable
fun LendingManagementScreen(
    lendingId: Uuid,
    model: LendingManagementViewModel = viewModel { LendingManagementViewModel(lendingId) },
    onBack: () -> Unit,
) {
    val users by model.users.collectAsState()
    val lending by model.lending.collectAsState()
    val scannedItems by model.scannedItems.collectAsState()
    val dismissedItems by model.dismissedItems.collectAsState()
    val receivedItems by model.receivedItems.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    lending?.let { lending ->
        val status = remember(lending) { lending.status() }
        if (status == Lending.Status.CONFIRMED || status == Lending.Status.TAKEN) {
            val hapticFeedback = LocalHapticFeedback.current

            val scanError by model.scanError.collectAsState()
            val scanSuccess by model.scanSuccess.collectAsState()
            val error by model.error.collectAsState()

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
                title = when (status) {
                    Lending.Status.CONFIRMED -> stringResource(Res.string.management_pickup_screen)
                    Lending.Status.TAKEN -> stringResource(Res.string.management_return_screen)
                    else -> "" // This case is already filtered
                },
                skipDialogTitle = when (status) {
                    Lending.Status.CONFIRMED -> stringResource(Res.string.management_pickup_screen_skip_warning_title)
                    Lending.Status.TAKEN -> stringResource(Res.string.management_return_screen_skip_warning_title)
                    else -> "" // This case is already filtered
                },
                skipDialogMessage = when (status) {
                    Lending.Status.CONFIRMED -> stringResource(Res.string.management_pickup_screen_skip_warning_message)
                    Lending.Status.TAKEN -> stringResource(Res.string.management_return_screen_skip_warning_message)
                    else -> "" // This case is already filtered
                },
                snackbarHostState = snackbarHostState,
                lending = lending,
                users = users.orEmpty(),
                scannedItems = scannedItems,
                dismissedItems = dismissedItems,
                onScanCode = model::onScan,
                onToggleItem = model::toggleItem,
                onCompleteRequest = when (status) {
                    Lending.Status.CONFIRMED -> model::pickup
                    Lending.Status.TAKEN -> model::`return`
                    else -> error("This case is already filtered")
                },
                onDeleteRequest = model::deleteLending,
                onBack = onBack,
                isItemToggleable = { itemId ->
                    if (status == Lending.Status.TAKEN) {
                        // In return screen, only items that not yet received can be toggled
                        receivedItems?.find { it.itemId == itemId } == null
                    } else {
                        true
                    }
                },
            )
        } else {
            LendingManagementScreen(
                lending = lending,
                onDeleteRequest = model::deleteLending,
                onConfirmRequest = model::confirmLending,
                onSkipMemoryRequest = model::skipMemory,
                users = users.orEmpty(),
                onBack = onBack,
            )
        }
    } ?: LoadingBox()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LendingManagementScreen(
    lending: ReferencedLending,
    users: List<UserData>,
    onDeleteRequest: () -> Job,
    onConfirmRequest: () -> Job,
    onSkipMemoryRequest: () -> Job,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val status = remember(lending) { lending.status() }

    var showingDeleteConfirmation by remember { mutableStateOf(false) }
    if (showingDeleteConfirmation) {
        DeleteDialog(
            title = stringResource(Res.string.lending_details_delete_confirm_title),
            message = stringResource(Res.string.lending_details_delete_confirm_message),
            onDelete = { onDeleteRequest().invokeOnCompletion { onBack() } },
            onDismissRequested = { showingDeleteConfirmation = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = { Text(stringResource(Res.string.lending_details_title)) },
                actions = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Left),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip { Text(stringResource(Res.string.lending_details_delete)) }
                        },
                    ) {
                        IconButton(
                            onClick = { showingDeleteConfirmation = true },
                        ) {
                            Icon(MaterialSymbols.Delete, stringResource(Res.string.lending_details_delete))
                        }
                    }
                    val isComplete = status == Lending.Status.MEMORY_SUBMITTED
                    if (isComplete) {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Left),
                            state = rememberTooltipState(),
                            tooltip = {
                                PlainTooltip { Text(stringResource(Res.string.lending_details_complete)) }
                            },
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.CheckCircle,
                                contentDescription = stringResource(Res.string.lending_details_complete),
                                tint = Color(0xFF58F158),
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumnWidthWrapper(Modifier.fillMaxSize().padding(paddingValues)) {
            lendingManagementScreenContent(
                lending = lending,
                snackbarHostState = snackbarHostState,
                users = users,
                onConfirmRequest = onConfirmRequest,
                onSkipMemoryRequest = onSkipMemoryRequest,
            )
        }
    }
}

fun LazyListScope.lendingManagementScreenContent(
    lending: ReferencedLending,
    snackbarHostState: SnackbarHostState,
    users: List<UserData>,
    onConfirmRequest: () -> Job,
    onSkipMemoryRequest: () -> Job,
    extraContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    item("general_details") {
        val status = remember(lending) { lending.status() }

        GeneralLendingDetails(lending) {
            GeneralLendingDetailsExtra(lending, snackbarHostState, users)

            if (status == Lending.Status.REQUESTED) {
                var isConfirming by remember { mutableStateOf(false) }
                ElevatedButton(
                    enabled = !isConfirming,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp, start = 12.dp, end = 12.dp),
                    onClick = {
                        isConfirming = true
                        onConfirmRequest().invokeOnCompletion { isConfirming = false }
                    },
                ) {
                    Icon(MaterialSymbols.Check, stringResource(Res.string.confirm))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.confirm))
                }
            }

            if (status == Lending.Status.RETURNED) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.memory_pending_lending),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    var skippingMemory by remember { mutableStateOf(false) }
                    TextButton(
                        enabled = !skippingMemory,
                        onClick = {
                            skippingMemory = true
                            onSkipMemoryRequest().invokeOnCompletion { skippingMemory = false }
                        },
                    ) {
                        Text(stringResource(Res.string.management_skip_memory))
                    }
                }
            }

            if (lending.memorySubmitted) {
                if (lending.memoryPdf == null) {
                    Text(
                        text = stringResource(Res.string.management_memory_pdf_not_available),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    MemoryViewButtons(lending)
                }
            }

            extraContent?.invoke(this)
        }
    }

    item("lendings") {
        LendingItems(lending)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingPickupReturnScreen(
    title: String,
    skipDialogTitle: String,
    skipDialogMessage: String,
    snackbarHostState: SnackbarHostState,
    lending: ReferencedLending,
    scannedItems: Set<Uuid>,
    dismissedItems: Set<Uuid>,
    users: List<UserData>,
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
                    log.i { "Barcode: ${result.barcode.data}, format: ${result.barcode.format}" }
                    onScanCode(result.barcode)
                    showingScanner = false
                }

                is BarcodeResult.OnFailed -> {
                    log.e(result.exception) { "Could not read barcode." }
                    scope.launch { snackbarHostState.showSnackbar(getString(Res.string.scanner_error)) }
                    showingScanner = false
                }

                BarcodeResult.OnCanceled -> {
                    log.d { "Scan cancelled" }
                    showingScanner = false
                }
            }
        }
    }

    var showingDeleteConfirmation by remember { mutableStateOf(false) }
    if (showingDeleteConfirmation) {
        DeleteDialog(
            title = stringResource(Res.string.lending_details_delete_confirm_title),
            message = stringResource(Res.string.lending_details_delete_confirm_message),
            onDelete = { onDeleteRequest().invokeOnCompletion { onBack() } },
            onDismissRequested = { showingDeleteConfirmation = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
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
                        Icon(MaterialSymbols.QrCodeScanner, null)
                    }
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Left),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip { Text(stringResource(Res.string.lending_details_delete)) }
                        },
                    ) {
                        IconButton(
                            onClick = { showingDeleteConfirmation = true },
                        ) {
                            Icon(MaterialSymbols.Delete, stringResource(Res.string.lending_details_delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val allItemsScanned = lending.items.all { it.id in scannedItems }
            AnimatedContent(
                targetState = allItemsScanned
            ) { areAllItemsScanned ->
                LendingFAB(skipDialogTitle, skipDialogMessage, areAllItemsScanned, onCompleteRequest)
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            LendingPickupReturnContent(lending, scannedItems, dismissedItems, snackbarHostState, users, onToggleItem, isItemToggleable)

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
                    MaterialSymbols.Check,
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
                        MaterialSymbols.KeyboardDoubleArrowRight,
                        stringResource(Res.string.management_pickup_screen_skip),
                        Modifier.sharedBounds(rememberSharedContentState("icon"), this@LendingFAB),
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralLendingDetailsExtra(
    lending: ReferencedLending,
    snackbarHostState: SnackbarHostState,
    users: List<UserData>,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val uriHandler = LocalUriHandler.current

    val user = remember(lending) { lending.user }
    val lendingUser = remember(user) { user.lendingUser!! }

    DataRow(
        icon = MaterialSymbols.Face,
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
            Icon(MaterialSymbols.ContactPhone, stringResource(Res.string.lending_details_copy_number))
            Text(stringResource(Res.string.lending_details_copy_number), Modifier.weight(1f).padding(start = 8.dp))
        }
        OutlinedButton(
            modifier = Modifier.weight(1f).padding(start = 4.dp),
            onClick = {
                uriHandler.openUri("mailto:${lending.user.email}")
            }
        ) {
            Icon(MaterialSymbols.Mail, stringResource(Res.string.lending_details_email))
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
            Icon(MaterialSymbols.Call, stringResource(Res.string.lending_details_call_user))
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

    val givenBy = lending.givenBy
    val givenAt = lending.givenAt
    if (givenBy != null && givenAt != null) {
        val givenAt = givenAt.toLocalDateTime(TimeZone.currentSystemDefault()).let { "${it.date} ${it.time}" }
        DataRow(
            icon = MaterialSymbols.CallMade,
            title = stringResource(Res.string.management_lending_given_by_title),
            text = stringResource(Res.string.management_lending_given_by_date, givenBy.fullName, givenAt),
        )
    }

    val items = lending.items
    val receptionDates = lending.receivedItems
        .groupBy { item -> item.receivedAt.toLocalDateTime(TimeZone.currentSystemDefault()).let { it.date to it.time.withoutSeconds() } }
    for ((receivedAt, receivedItems) in receptionDates) {
        val receivedAtStr = "${receivedAt.first} ${receivedAt.second}"
        val returnedTo = receivedItems
            .mapNotNull { users.find { user -> user.sub == it.receivedBy } }
            .toSet()
            .joinToString { it.fullName }
        val items = receivedItems
            .mapNotNull { items.find { item -> item.id == it.itemId } }
            .groupBy { it.type }
            .toList()
            .joinToString("\n") { (type, items) -> "- ${type.displayName} (${items.size})" }
        DataRow(
            icon = MaterialSymbols.CallMade,
            title = stringResource(Res.string.management_lending_returned_to_title, returnedTo),
            text = stringResource(Res.string.management_lending_returned_to_data, receivedAtStr, items),
        )
    }

    Spacer(Modifier.height(12.dp))
}

@Composable
private fun LendingPickupReturnContent(
    lending: ReferencedLending,
    scannedItems: Set<Uuid>,
    dismissedItems: Set<Uuid>,
    snackbarHostState: SnackbarHostState,
    users: List<UserData>,
    onToggleItem: (Uuid) -> Unit,
    isItemToggleable: (Uuid) -> Boolean = { true },
) {
    val items = lending.items

    GeneralLendingDetails(lending) { GeneralLendingDetailsExtra(lending, snackbarHostState, users) }

    if (PlatformNFC.isSupported) {
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Icon(
                    MaterialSymbols.Nfc,
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
                MaterialSymbols.CheckCircle
            } else if (dismissed) {
                MaterialSymbols.Cancel
            } else {
                MaterialSymbols.Help
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
