package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.automirrored.filled.LastPage
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.FreeCancellation
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.ktor.http.ContentType
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.fetchDocumentFilePath
import org.centrexcursionistalcoi.app.data.rememberImageFile
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.dialog.DeleteDialog
import org.centrexcursionistalcoi.app.ui.reusable.AsyncByteImage
import org.centrexcursionistalcoi.app.ui.reusable.CardWithIcon
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.viewmodel.LendingDetailsModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingDetailsScreen(
    lendingId: Uuid,
    model: LendingDetailsModel = viewModel { LendingDetailsModel(lendingId) },
    onMemoryEditorRequested: () -> Unit,
    onBack: () -> Unit
) {
    val lending by model.lending.collectAsState()
    val memoryUploadProgress by model.memoryUploadProgress.collectAsState()

    lending?.let { lending ->
        LendingDetailsScreen(
            lending = lending,
            onCancelRequest = model::cancelLending,
            memoryUploadProgress = memoryUploadProgress,
            onMemoryUploadRequest = model::submitMemory,
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
    memoryUploadProgress: Progress? = null,
    onMemoryUploadRequest: (PlatformFile) -> Unit = {},
    onMemoryEditorRequest: () -> Unit = {},
    onBack: () -> Unit,
) {
    var showingCancelConfirmation by remember { mutableStateOf(false) }
    if (showingCancelConfirmation) {
        DeleteDialog(
            title = stringResource(Res.string.lending_details_cancel_confirm_title),
            message = stringResource(Res.string.lending_details_cancel_confirm_message),
            buttonText = stringResource(Res.string.lending_details_cancel),
            onDelete = { onCancelRequest().invokeOnCompletion { onBack() } },
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
                                Icon(Icons.Default.FreeCancellation, stringResource(Res.string.lending_details_cancel))
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
                                imageVector = Icons.Default.CheckCircle,
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
        LazyColumnWidthWrapper(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            val isPendingConfirmation = lending.status() == Lending.Status.REQUESTED
            if (isPendingConfirmation) {
                item(key = "pending_confirmation") {
                    CardWithIcon(
                        title = stringResource(Res.string.lending_details_confirmation_pending_title),
                        message = stringResource(Res.string.lending_details_confirmation_pending_message),
                        icon = Icons.Default.Pending,
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
                        icon = Icons.Default.Inventory2,
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
                        icon = Icons.AutoMirrored.Default.AssignmentReturn,
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
                        icon = Icons.AutoMirrored.Default.AssignmentReturn,
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
                        icon = Icons.AutoMirrored.Filled.NoteAdd,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).padding(horizontal = 16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    )
                }

                item(key = "memory_actions") {
                    MemoryActions(
                        onUploadRequest = onMemoryUploadRequest,
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
                LendingItems(lending)
            }
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
                    text = lending.to.toString(),
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

@Composable
fun LendingItems(
    lending: ReferencedLending,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.lending_details_items),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )

        val items = lending.items.groupBy { it.type }
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

@Composable
fun MemoryActions(
    memoryUploadProgress: Progress? = null,
    onUploadRequest: (PlatformFile) -> Unit,
    onEditorRequest: () -> Unit,
) {
    val filePickerLauncher = rememberFilePickerLauncher(type = FileKitType.File("pdf")) { file ->
        if (file != null) onUploadRequest(file)
    }

    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.lending_details_memory),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { filePickerLauncher.launch() },
            ) {
                Icon(
                    imageVector = Icons.Default.UploadFile,
                    contentDescription = stringResource(Res.string.memory_pick)
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.memory_pick))
            }
            Spacer(Modifier.size(8.dp))
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onEditorRequest,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = stringResource(Res.string.memory_editor)
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.memory_editor))
            }
        }

        if (memoryUploadProgress != null) {
            Spacer(Modifier.height(8.dp))
            LinearLoadingIndicator(memoryUploadProgress)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun MemoryVisualization(
    lending: ReferencedLending
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.lending_details_memory_view),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(12.dp)
        )

        val document = lending.memoryDocument
        if (document != null) {
            if (PlatformOpenFileLogic.isSupported) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    onClick = {
                        val path = runBlocking { lending.fetchDocumentFilePath() }
                        PlatformOpenFileLogic.open(path, ContentType.Application.Pdf)
                    }
                ) {
                    Text(
                        text = stringResource(Res.string.management_view_memory),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.lending_details_memory_view_unsupported),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        } else {
            // TODO: Implement viewing memory from text
            Text("Visualization not implemented")
        }
    }
}
