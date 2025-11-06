package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.ui.screen.shared.LendingPickupReturnScreen
import org.centrexcursionistalcoi.app.viewmodel.LendingReturnViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingReturnScreen(
    lendingId: Uuid,
    model: LendingReturnViewModel = viewModel { LendingReturnViewModel(lendingId) },
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    val receivedItems by model.receivedItems.collectAsState()

    LendingPickupReturnScreen(
        title = stringResource(Res.string.management_return_screen),
        skipDialogTitle = stringResource(Res.string.management_return_screen_skip_warning_title),
        skipDialogMessage = stringResource(Res.string.management_return_screen_skip_warning_message),
        model = model,
        onBack = onBack,
        onCompleteRequest = { model::`return` { onComplete() } },
        isItemToggleable = { itemId ->
            // In return screen, only items that not yet received can be toggled
            receivedItems?.find { it.itemId == itemId } == null
        },
    )
}
