package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.ui.screen.shared.LendingPickupReturnScreen
import org.centrexcursionistalcoi.app.viewmodel.LendingReturnViewModel

@Composable
fun LendingReturnScreen(
    lendingId: Uuid,
    model: LendingReturnViewModel = viewModel { LendingReturnViewModel(lendingId) },
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    val lending by model.lending.collectAsState()
    val scannedItems by model.scannedItems.collectAsState()
    val dismissedItems by model.dismissedItems.collectAsState()
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

    LendingPickupReturnScreen(
        snackbarHostState = snackbarHostState,
        lending = lending,
        scannedItems = scannedItems,
        dismissedItems = dismissedItems,
        onScanCode = model::onScan,
        onToggleItem = model::toggleItem,
        onCompleteRequest = model::`return`,
        onDeleteRequest = model::deleteLending,
        onBack = onBack,
        onComplete = onComplete,
    )
}
