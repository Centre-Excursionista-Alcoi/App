package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.ktor.http.ContentType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.documentFilePath
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.ui.platform.calculateWindowSizeClass
import org.centrexcursionistalcoi.app.ui.reusable.AdaptiveVerticalGrid
import org.centrexcursionistalcoi.app.ui.reusable.ListCard
import org.centrexcursionistalcoi.app.ui.utils.unknown
import org.centrexcursionistalcoi.app.viewmodel.LendingsManagementViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingsManagementScreen(
    model: LendingsManagementViewModel = viewModel { LendingsManagementViewModel() },
    onLendingPickupRequest: (ReferencedLending) -> Unit,
    onBack: () -> Unit
) {
    val lendings by model.lendings.collectAsState()

    LendingsManagementScreen(
        lendings = lendings,
        onConfirmRequest = model::confirm,
        onPickupRequest = onLendingPickupRequest,
        onReturnRequest = model::`return`,
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
                    IconButton(
                        onClick = onBack
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                title = { Text("Lendings") }
            )
        }
    ) { paddingValues ->
        val windowSizeClass = calculateWindowSizeClass()
        AdaptiveVerticalGrid(
            windowSizeClass = windowSizeClass,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (unconfirmedLendings.isNotEmpty()) item(key = "unconfirmed_lendings") {
                UnconfirmedLendingsCard(unconfirmedLendings, onConfirmRequest)
            }
            if (pendingPickupLendings.isNotEmpty()) item(key = "pending_pickup_lendings") {
                PendingPickupLendingsCard(pendingPickupLendings, onPickupRequest)
            }
            if (pendingReturnLendings.isNotEmpty()) item(key = "pending_return_lendings") {
                PendingReturnLendingsCard(pendingReturnLendings, onReturnRequest)
            }
            if (pendingMemoryLendings.isNotEmpty()) item(key = "pending_memory_lendings") {
                PendingMemoryLendingsCard(pendingMemoryLendings)
            }
            if (completedLendings.isNotEmpty()) item(key = "completed_lendings") {
                CompleteLendingsCard(completedLendings)
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
fun UnconfirmedLendingsCard(
    lendings: List<ReferencedLending>,
    onConfirmRequest: (ReferencedLending) -> Unit,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_unconfirmed_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text("User: ${lending.user.username}")
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            Text("User: ${lending.user.username}")
            Text("Items:")
            for ((type, items) in lending.items.groupBy { it.type }) {
                Text("- ${type.displayName}: ${items.size} unit(s)")
            }

            HorizontalDivider()

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onConfirmRequest(lending)
                    dismiss()
                }
            ) { Text("Confirm") }
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
            Text("User: ${lending.user.username}")
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
            Text("User: ${lending.user.username}")
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            val items = lending.items

            Text("User: ${lending.user.username}")
            Text("Items:")
            for ((type, items) in items.groupBy { it.type }) {
                Text("- ${type.displayName}: ${items.size} unit(s)")
            }

            HorizontalDivider()

            val givenBy = lending.givenBy
            val givenAt = lending.givenAt?.toLocalDateTime(TimeZone.currentSystemDefault())
            Text("Given by: ${givenBy?.username ?: "Unknown"} at ${givenAt ?: "Unknown time"}")

            HorizontalDivider()

            Text("When pressing the button above, you are confirming that the user has returned the items in good condition.")
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onReturnRequest(lending)
                    dismiss()
                }
            ) { Text("Return") }
        }
    )
}

@Composable
fun PendingMemoryLendingsCard(
    lendings: List<ReferencedLending>,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_pending_memory_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.username))
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.username))
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
            Text(stringResource(Res.string.management_lending_returned_to, givenBy?.username ?: unknown(), givenAt?.toString() ?: unknown()))
        }
    )
}

@Composable
fun CompleteLendingsCard(
    lendings: List<ReferencedLending>,
) {
    ListCard(
        list = lendings,
        titleResource = Res.string.management_completed_lendings,
        emptyTextResource = Res.string.management_no_lendings,
        displayName = { it.id.toString() },
        supportingContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.username))
        },
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        detailsDialogContent = { lending ->
            Text(stringResource(Res.string.management_lending_user, lending.user.username))
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
            Text(stringResource(Res.string.management_lending_returned_to, givenByUser?.username ?: unknown(), givenAt?.toString() ?: unknown()))

            if (PlatformOpenFileLogic.supported && lending.memoryDocument != null) {
                HorizontalDivider()

                TextButton(
                    onClick = {
                        val path = lending.documentFilePath()
                        PlatformOpenFileLogic.open(path, ContentType.Application.Pdf)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text(stringResource(Res.string.management_view_memory)) }
            }
        }
    )
}
