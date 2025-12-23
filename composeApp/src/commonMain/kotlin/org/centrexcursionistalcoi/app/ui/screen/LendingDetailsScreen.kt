package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.centrexcursionistalcoi.app.data.*
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItem.Companion.referenced
import org.centrexcursionistalcoi.app.data.ReferencedInventoryItemType.Companion.referenced
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.utils.toUuid
import org.centrexcursionistalcoi.app.viewmodel.FileProviderModel
import org.centrexcursionistalcoi.app.viewmodel.LendingDetailsModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Composable
fun LendingDetailsScreen(
    lendingId: Uuid,
    model: LendingDetailsModel = viewModel { LendingDetailsModel(lendingId) },
    onMemoryEditorRequested: () -> Unit,
    onBack: () -> Unit
) {
    val lending by model.lending.collectAsState()

    lending?.let { lending ->
        LendingDetailsScreen(
            lending = lending,
            onCancelRequest = model::cancelLending,
            onMemoryEditorRequest = onMemoryEditorRequested,
            onBack = onBack
        )
    } ?: LoadingBox()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LendingDetailsScreen(
    lending: ReferencedLending,
    onCancelRequest: () -> Job,
    onMemoryEditorRequest: () -> Unit = {},
    onBack: () -> Unit,
) {
    var showingCancelConfirmation by remember { mutableStateOf(false) }
    if (showingCancelConfirmation) {
        DeleteDialog(
            title = stringResource(Res.string.lending_details_cancel_confirm_title),
            message = stringResource(Res.string.lending_details_cancel_confirm_message),
            buttonText = stringResource(Res.string.lending_details_cancel),
            onDelete = { onCancelRequest().also { it.invokeOnCompletion { onBack() } } },
            onDismissRequested = { showingCancelConfirmation = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = { Text(stringResource(Res.string.lending_details_title)) },
                actions = {
                    val canBeCancelled = lending.status() == Lending.Status.REQUESTED || lending.status() == Lending.Status.CONFIRMED
                    if (canBeCancelled) {
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
                                Icon(MaterialSymbols.FreeCancellation, stringResource(Res.string.lending_details_cancel))
                            }
                        }
                    }
                    val isComplete = lending.status() == Lending.Status.MEMORY_SUBMITTED
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
        }
    ) { paddingValues ->
        LendingDetailsScreen_Content(
            lending = lending,
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            onMemoryEditorRequest = onMemoryEditorRequest,
        )
    }
}

@Composable
fun LendingDetailsScreen_Content(
    lending: ReferencedLending,
    modifier: Modifier = Modifier,
    onMemoryEditorRequest: () -> Unit,
) {
    LazyColumnWidthWrapper(modifier) {
        val isPendingConfirmation = lending.status() == Lending.Status.REQUESTED
        if (isPendingConfirmation) {
            item(key = "pending_confirmation") {
                CardWithIcon(
                    title = stringResource(Res.string.lending_details_confirmation_pending_title),
                    message = stringResource(Res.string.lending_details_confirmation_pending_message),
                    icon = MaterialSymbols.Pending,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).padding(horizontal = 16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            }
        }

        val isPendingPickup = lending.status() == Lending.Status.CONFIRMED
        if (isPendingPickup) {
            item(key = "pending_pickup") {
                CardWithIcon(
                    title = stringResource(Res.string.lending_details_pickup_pending_title),
                    message = stringResource(Res.string.lending_details_pickup_pending_message),
                    icon = MaterialSymbols.Inventory2,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).padding(horizontal = 16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            }
        }

        val isPendingReturn = lending.status() == Lending.Status.TAKEN
        val activityFinished = lending.to.atStartOfDayIn(TimeZone.currentSystemDefault()) < Clock.System.now()
        if (isPendingReturn && activityFinished) item(key = "pending_return") {
            val isIncompleteReturn = lending.receivedItems.isNotEmpty() && lending.receivedItems.size < lending.items.size
            if (isIncompleteReturn) {
                CardWithIcon(
                    title = stringResource(Res.string.lending_details_incomplete_return_title),
                    message = stringResource(Res.string.lending_details_incomplete_return_message),
                    icon = MaterialSymbols.AssignmentReturn,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).padding(horizontal = 16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            } else {
                CardWithIcon(
                    title = stringResource(Res.string.lending_details_return_pending_title),
                    message = stringResource(Res.string.lending_details_return_pending_message),
                    icon = MaterialSymbols.AssignmentReturn,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).padding(horizontal = 16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            }
        }

        val isMemoryPending = lending.status() == Lending.Status.RETURNED
        if (isMemoryPending) {
            item(key = "memory_pending") {
                CardWithIcon(
                    title = stringResource(Res.string.lending_details_memory_pending_title),
                    message = stringResource(Res.string.lending_details_memory_pending_message),
                    icon = MaterialSymbols.NoteAdd,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).padding(horizontal = 16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                )
            }

            item(key = "memory_actions") {
                MemoryActions(
                    onEditorRequest = onMemoryEditorRequest,
                )
            }
        }

        val isMemorySubmitted = lending.status() == Lending.Status.MEMORY_SUBMITTED
        if (isMemorySubmitted) item("memory_visualization") {
            MemoryVisualization(lending)
        }

        item("basic_details") {
            GeneralLendingDetails(lending)
        }

        item("items") {
            LendingItems(lending, false)
        }
    }
}

@Composable
fun GeneralLendingDetails(
    lending: ReferencedLending,
    extraContent: (@Composable ColumnScope.() -> Unit)? = null,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.lending_details_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )
        DataRow(
            icon = MaterialSymbols.Numbers,
            titleRes = Res.string.lending_details_id,
            text = lending.id.toString(),
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 8.dp)) {
            Icon(MaterialSymbols.FirstPage, null, Modifier.padding(end = 8.dp))
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
            Icon(MaterialSymbols.LastPage, null, Modifier.padding(end = 8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.lending_details_until),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = lending.to.toString(),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        lending.notes?.let { notes ->
            DataRow(
                icon = MaterialSymbols.Notes,
                titleRes = Res.string.lending_details_notes,
                text = notes,
            )
        }

        extraContent?.invoke(this) ?: Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun DataRow(
    titleRes: StringResource,
    text: String,
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    DataRow(stringResource(titleRes), text, icon, contentDescription, onClick)
}

@Composable
fun DataRow(
    title: String,
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun LendingItems(
    lending: ReferencedLending,
    isManagement: Boolean,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.lending_details_items),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )

        val items = remember(lending) { lending.items.groupBy { it.type } }
        for ((type, items) in items) {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                val image by type.rememberImageFile()
                AsyncByteImage(image, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))

                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(Res.string.inventory_item_amount, items.size),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    if (isManagement) {
                        Spacer(Modifier.height(8.dp))

                        for (item in items) {
                            Text(
                                text = item.id.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemoryActions(
    onEditorRequest: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.lending_details_memory),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            onClick = onEditorRequest,
        ) {
            Icon(
                imageVector = MaterialSymbols.Article,
                contentDescription = stringResource(Res.string.memory_editor)
            )
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.memory_editor))
        }
    }
}

@Composable
fun MemoryVisualization(
    lending: ReferencedLending,
) {
    lending.memoryPdf ?: return

    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.lending_details_memory_view),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )

        MemoryViewButtons(lending)
    }
}

@Composable
fun MemoryViewButtons(
    lending: ReferencedLending,
    fpm: FileProviderModel = viewModel { FileProviderModel() },
) {
    val memoryPdf = lending.memoryPdf ?: return

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (PlatformShareLogic.isSupported) {
            IconButton(
                onClick = {
                    fpm.shareFile { lending.fetchFilePath(memoryPdf) }
                },
            ) {
                Icon(MaterialSymbols.Share, stringResource(Res.string.share))
            }
        }
        if (PlatformOpenFileLogic.isSupported) {
            OutlinedButton(
                onClick = {
                    fpm.openFile { lending.fetchFilePath(memoryPdf) }
                },
                modifier = Modifier.weight(1f).padding(start = 8.dp)
            ) {
                Text(stringResource(Res.string.insurance_view_document))
            }
        }
    }
}

private val previewItemType = InventoryItemType(
    id = "3b69c54e-3465-4a1d-a194-29dc64058e4a".toUuid(),
    displayName = "Test Item",
    description = null,
    categories = null,
    department = null,
    image = null,
).referenced(emptyList())

private val previewItem = InventoryItem(
    id = "8c9ccff9-ec17-4918-9327-08f7de3576d3".toUuid(),
    variation = null,
    type = previewItemType.id,
    nfcId = null,
    manufacturerTraceabilityCode = null,
).referenced(previewItemType)

private val previewUserData = UserData(
    sub = "abc",
    fullName = "Example User",
    email = "user@example.com",
    groups = listOf(),
    departments = emptyList(),
    memberNumber = 123u,
    lendingUser = null,
    insurances = emptyList(),
    isDisabled = false,
)

private val previewLending = Lending(
    id = "290ecb88-5676-4baf-971b-4488e174b942".toUuid(),
    userSub = previewUserData.sub,
    timestamp = Instant.fromEpochSeconds(1766509399),
    confirmed = false,
    taken = false,
    givenBy = null,
    givenAt = null,
    returned = false,
    receivedItems = emptyList(),
    memorySubmitted = false,
    memorySubmittedAt = null,
    memory = null,
    memoryPdf = null,
    memoryReviewed = false,
    from = LocalDate(2025, 12, 23),
    to = LocalDate(2025, 12, 25),
    notes = null,
    items = listOf(previewItem.referencedEntity)
).referenced(listOf(previewUserData), listOf(previewItemType))

@Preview(showBackground = true)
@Composable
private fun LendingDetailsScreen_Content_Preview() {
    LendingDetailsScreen_Content(
        lending = previewLending,
        onMemoryEditorRequest = {},
    )
}
