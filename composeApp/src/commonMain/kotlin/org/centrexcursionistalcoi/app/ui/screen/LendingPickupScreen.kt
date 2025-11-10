package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlin.uuid.Uuid
import org.centrexcursionistalcoi.app.ui.screen.shared.LendingPickupReturnScreen
import org.centrexcursionistalcoi.app.viewmodel.LendingPickupViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingPickupScreen(
    lendingId: Uuid,
    model: LendingPickupViewModel = viewModel { LendingPickupViewModel(lendingId) },
    onBack: () -> Unit,
    onComplete: () -> Unit,
) {
    LendingPickupReturnScreen(
        title = stringResource(Res.string.management_pickup_screen),
        skipDialogTitle = stringResource(Res.string.management_pickup_screen_skip_warning_title),
        skipDialogMessage = stringResource(Res.string.management_pickup_screen_skip_warning_message),
        model = model,
        onBack = onBack,
        onCompleteRequest = { model::pickup { onComplete() } },
    )
}
