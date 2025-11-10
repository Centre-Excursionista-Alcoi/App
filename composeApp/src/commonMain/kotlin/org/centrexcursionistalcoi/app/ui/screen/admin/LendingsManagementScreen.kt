package org.centrexcursionistalcoi.app.ui.screen.admin

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.fetchDocumentFilePath
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.ui.data.DialogContext
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.ListCard
import org.centrexcursionistalcoi.app.ui.reusable.OutlinedButtonWithIcon
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.utils.unknown
import org.centrexcursionistalcoi.app.viewmodel.FileProviderModel
import org.centrexcursionistalcoi.app.viewmodel.LendingsManagementViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingsManagementScreen(
    model: LendingsManagementViewModel = viewModel { LendingsManagementViewModel() },
    onLendingPickupRequest: (ReferencedLending) -> Unit,
    onLendingReturnRequest: (ReferencedLending) -> Unit,
    onBack: () -> Unit
) {
    val lendings by model.lendings.collectAsState()

    LendingsManagementScreen(
        lendings = lendings,
        onConfirmRequest = model::confirm,
        onPickupRequest = onLendingPickupRequest,
        onReturnRequest = onLendingReturnRequest,
        onDeleteRequest = model::delete,
        onSkipMemoryRequest = model::skipMemory,
        onBack = onBack
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingsManagementScreen(
    lendings: List<ReferencedLending>?,
    onConfirmRequest: (ReferencedLending) -> Unit,
    onPickupRequest: (ReferencedLending) -> Unit,
    onReturnRequest: (ReferencedLending) -> Unit,
    onSkipMemoryRequest: (ReferencedLending) -> Unit,
    onDeleteRequest: (ReferencedLending) -> Job,
    onBack: () -> Unit
) {
    val unconfirmedLendings = remember(lendings) { lendings?.filter { it.status() == Lending.Status.REQUESTED }.orEmpty() }
    val pendingPickupLendings = remember(lendings) { lendings?.filter { it.status() == Lending.Status.CONFIRMED }.orEmpty() }
    val pendingReturnLendings = remember(lendings) { lendings?.filter { it.status() == Lending.Status.TAKEN }.orEmpty() }
    val pendingMemoryLendings = remember(lendings) { lendings?.filter { it.status() == Lending.Status.RETURNED }.orEmpty() }
    val completedLendings = remember(lendings) { lendings?.filter { it.status() in listOf(Lending.Status.MEMORY_SUBMITTED, Lending.Status.COMPLETE) }.orEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onBack)
                },
                title = { Text("Lendings") }
            )
        }
    ) { paddingValues ->
        var deletingLending by remember { mutableStateOf<ReferencedLending?>(null) }
        deletingLending?.let { lending ->
            DeleteDialog(
                item = lending,
                displayName = { it.id.toString() },
                onDelete = { onDeleteRequest(lending) },
                onDismissRequested = { deletingLending = null },
            )
        }

        val windowSizeClass = calculateWindowSizeClass()
        AdaptiveVerticalGrid(
            windowSizeClass = windowSizeClass,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (unconfirmedLendings.isNotEmpty()) item(key = "unconfirmed_lendings") {
                UnconfirmedLendingsCard(unconfirmedLendings, onConfirmRequest) { deletingLending = it }
            }
            if (pendingPickupLendings.isNotEmpty()) item(key = "pending_pickup_lendings") {
                PendingPickupLendingsCard(pendingPickupLendings, onPickupRequest)
            }
            if (pendingReturnLendings.isNotEmpty()) item(key = "pending_return_lendings") {
                PendingReturnLendingsCard(pendingReturnLendings, onReturnRequest)
            }
            if (pendingMemoryLendings.isNotEmpty()) item(key = "pending_memory_lendings") {
                PendingMemoryLendingsCard(pendingMemoryLendings, onSkipMemoryRequest) { deletingLending = it }
            }
            if (completedLendings.isNotEmpty()) item(key = "completed_lendings") {
                CompleteLendingsCard(completedLendings) { deletingLending = it }
            }

            if ((unconfirmedLendings + pendingPickupLendings + pendingReturnLendings + pendingMemoryLendings + completedLendings).isEmpty()) {
                item(key = "no_lendings") {
                    Text(
                        text = stringResource(Res.string.management_no_lendings),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
context(dialog: DialogContext?)
fun LendingsCardActions(
    onDeleteRequest: () -> Unit,
    vararg actions: (@Composable RowScope.() -> Unit)?,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        OutlinedButtonWithIcon(
            icon = Icons.Default.Delete,
            text = stringResource(Res.string.delete),
            modifier = Modifier.weight(1f),
            onClick = {
                onDeleteRequest()
                dialog?.dismiss()
            }
        )
        for (action in actions) {
            action ?: continue
            Spacer(modifier = Modifier.width(8.dp))
            action()
        }
    }
}

@Composable
fun UnconfirmedLendingsCard(
    lendings: List<ReferencedLending>,
    onConfirmRequest: (ReferencedLending) -> Unit,
    onDeleteRequest: (ReferencedLending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_unconfirmed_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
            Text(stringResource(Res.string.lending_details_items_title))
            for ((type, items) in lending.items.groupBy { it.type }) {
                Text(
                    text = pluralStringResource(Res.plurals.lending_details_item_row, items.size, type.displayName, items.size)
                )
            }

            HorizontalDivider()

            LendingsCardActions(
                onDeleteRequest = { onDeleteRequest(lending) },
                {
                    OutlinedButtonWithIcon(
                        icon = Icons.Default.Check,
                        text = stringResource(Res.string.confirm),
                        modifier = Modifier.weight(1f),
                    ) {
                        onConfirmRequest(lending)
                        this@ListCard.dismiss()
                    }
                }
            )
        }
    )
}

@Composable
fun PendingPickupLendingsCard(
    lendings: List<ReferencedLending>,
    onPickupRequest: (ReferencedLending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_pending_pickup_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = { onPickupRequest(it) },
    )
}

@Composable
fun PendingReturnLendingsCard(
    lendings: List<ReferencedLending>,
    onReturnRequest: (ReferencedLending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_pending_return_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        onClick = onReturnRequest,
    )
}

@Composable
fun PendingMemoryLendingsCard(
    lendings: List<ReferencedLending>,
    onSkipMemoryRequest: (ReferencedLending) -> Unit,
    onDeleteRequest: (ReferencedLending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_pending_memory_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
            Text(stringResource(Res.string.lending_details_items_title))
            for ((type, items) in lending.items.groupBy { it.type }) {
                Text(
                    pluralStringResource(
                        Res.plurals.lending_details_item_row, items.size, type.displayName, items.size
                    )
                )
            }

            HorizontalDivider()

            val givenBy = lending.givenBy
            val givenAt = lending.givenAt?.toLocalDateTime(TimeZone.currentSystemDefault())
            Text(stringResource(Res.string.management_lending_returned_to, givenBy?.fullName ?: unknown(), givenAt?.toString() ?: unknown()))

            LendingsCardActions(
                onDeleteRequest = { onDeleteRequest(lending) },
                {
                    OutlinedButtonWithIcon(
                        icon = Icons.Default.Check,
                        text = stringResource(Res.string.management_skip_memory),
                        modifier = Modifier.weight(1f),
                    ) {
                        onSkipMemoryRequest(lending)
                        this@ListCard.dismiss()
                    }
                }
            )
        }
    )
}

@Composable
fun CompleteLendingsCard(
    lendings: List<ReferencedLending>,
    fpm: FileProviderModel = viewModel { FileProviderModel() },
    onDeleteRequest: (ReferencedLending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_completed_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.fullName))
            Text(stringResource(Res.string.lending_details_items_title))
            for ((type, items) in lending.items.groupBy { it.type }) {
                Text(
                    pluralStringResource(
                        Res.plurals.lending_details_item_row, items.size, type.displayName, items.size
                    )
                )
            }

            HorizontalDivider()

            val givenByUser = lending.givenBy
            val givenAt = lending.givenAt?.toLocalDateTime(TimeZone.currentSystemDefault())
            Text(stringResource(Res.string.management_lending_returned_to, givenByUser?.fullName ?: unknown(), givenAt?.toString() ?: unknown()))

            LendingsCardActions(
                onDeleteRequest = { onDeleteRequest(lending) },
                if (PlatformOpenFileLogic.isSupported && lending.memoryDocument != null) {
                    {
                        OutlinedButtonWithIcon(
                            icon = Icons.Default.FileOpen,
                            text = stringResource(Res.string.management_view_memory),
                            modifier = Modifier.weight(1f),
                        ) {
                            fpm.openFile { lending.fetchDocumentFilePath() }
                        }
                    }
                } else null,
            )

            fpm.progress.LinearLoadingIndicator()
        }
    )
}
