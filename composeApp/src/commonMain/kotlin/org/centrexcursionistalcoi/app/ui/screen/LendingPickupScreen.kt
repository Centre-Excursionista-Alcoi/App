package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.aakira.napier.Napier
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.ReferencedLending.Companion.referenced
import org.centrexcursionistalcoi.app.platform.PlatformNFC
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.LendingPickupViewModel
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.ncgroup.kscan.Barcode
import org.ncgroup.kscan.BarcodeFormat
import org.ncgroup.kscan.BarcodeResult
import org.ncgroup.kscan.ScannerView

@Composable
fun LendingPickupScreen(
    lendingId: Uuid,
    model: LendingPickupViewModel = viewModel { LendingPickupViewModel(lendingId) },
    onBack: () -> Unit
) {
    val lending by model.lending.collectAsState()
    val scannedItems by model.scannedItems.collectAsState()

    LifecycleStartEffect(Unit) {
        model.startNfc()
        onStopOrDispose {
            model.stopNfc()
        }
    }

    LendingPickupScreen(
        lending = lending,
        scannedItems = scannedItems,
        onScanCode = model::onScan,
        onBack = onBack
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingPickupScreen(
    lending: ReferencedLending?,
    scannedItems: Set<Uuid>,
    onScanCode: (Barcode) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showingScanner by remember { mutableStateOf(false) }
    if (showingScanner) {
        ScannerView(
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
                }

                is BarcodeResult.OnFailed -> {
                    Napier.e(result.exception) { "Could not read barcode." }
                    scope.launch { snackbarHostState.showSnackbar(getString(Res.string.scanner_error)) }
                }

                BarcodeResult.OnCanceled -> {
                    Napier.d { "Scan cancelled" }
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
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        if (lending == null) LoadingBox(paddingValues)
        else Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())) {
            LendingPickupContent(lending, scannedItems)
        }
    }
}

@Composable
private fun LendingPickupContent(
    lending: ReferencedLending,
    scannedItems: Set<Uuid>,
    supportsNFC: Boolean = PlatformNFC.supportsNFC,
) {
    val items = lending.items

    if (supportsNFC) {
        OutlinedCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Icon(
                Icons.Default.Nfc,
                contentDescription = null,
                modifier = Modifier
                    .padding(16.dp)
            )
            Text(
                text = "NFC reader",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, end = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Place your device near the NFC tag to register the item automatically.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, end = 16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        for (item in items) {
            ListItem(
                headlineContent = {
                    Text(
                        item.type.displayName
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = if (item.id in scannedItems) {
                            Icons.Default.CheckCircleOutline
                        } else {
                            Icons.Default.RemoveCircleOutline
                        },
                        contentDescription = null,
                        tint = if (item.id in scannedItems) {
                            Color(0xFF4CAF50) // Green for scanned
                        } else {
                            Color(0xFF368CF4) // Blue for not scanned
                        }
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LendingPickupContent_Preview() {
    LendingPickupContent(
        lending = Lending(
            id = Uuid.parse("123e4567-e89b-12d3-a456-426614174000"),
            userSub = "user-sub-example",
            timestamp = Instant.fromEpochMilliseconds(1625079600000),
            confirmed = true,

            taken = false,
            givenBy = null,
            givenAt = null,

            returned = false,
            receivedBy = null,
            receivedAt = null,

            memorySubmitted = false,
            memorySubmittedAt = null,
            memoryDocument = null,
            memoryReviewed = false,

            from = LocalDate(2025, 10, 21),
            to = LocalDate(2025, 10, 25),
            notes = null,
            items = listOf(),
        ).referenced(listOf()),
        scannedItems = emptySet(),
        supportsNFC = true,
    )
}
