package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.data.fetchDocumentFilePath
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.ui.reusable.LinearLoadingIndicator
import org.centrexcursionistalcoi.app.viewmodel.FileProviderModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingDetailsDialog(
    lending: ReferencedLending,
    onCancelRequest: () -> Unit,
    memoryUploadProgress: Progress?,
    onMemorySubmitted: ((PlatformFile) -> Job)?,
    onMemoryEditorRequested: (() -> Unit)?,
    onDismissRequest: () -> Unit,
    fpm: FileProviderModel = viewModel { FileProviderModel() },
) {
    var isSubmittingMemory by remember { mutableStateOf(false) }
    val filePickLauncher = rememberFilePickerLauncher(FileKitType.File("pdf")) { file ->
        if (file == null) return@rememberFilePickerLauncher
        isSubmittingMemory = true
        onMemorySubmitted?.invoke(file)?.invokeOnCompletion {
            isSubmittingMemory = false
            onDismissRequest()
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.lending_details_title)) },
        text = {
            Column {
                Text("Lending ID: ${lending.id}")
                Text("From: ${lending.from}")
                Text("To: ${lending.to}")

                val status = lending.status()
                when (status) {
                    Lending.Status.REQUESTED -> Text("Status: Pending")
                    Lending.Status.TAKEN -> Text("Taken on: ${lending.givenAt}")
                    Lending.Status.CONFIRMED -> Text("Status: Confirmed")
                    Lending.Status.RETURNED -> {
                        if (lending.receivedItems.isEmpty()) {
                            Text("Status: ERROR! No items returned")
                            return@Column
                        } else if (lending.receivedItems.size == 1) {
                            val received = lending.receivedItems.first()
                            Text("Returned on: ${received.receivedAt}")
                        } else {
                            val text = lending.receivedItems
                                .groupBy { it.receivedAt }
                                .map { (date, items) ->
                                    "- ${items.size} items on $date"
                                }
                                .joinToString("\n")
                            Text("Returned:\n$text")
                        }
                    }

                    else -> { /* nothing */ }
                }
                if (status == Lending.Status.RETURNED) {
                    Text(
                        text = stringResource(resource = Res.string.memory_pending_lending),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )

                    OutlinedButton(
                        enabled = !isSubmittingMemory,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { filePickLauncher.launch() }
                    ) {
                        Icon(Icons.Default.AttachFile, stringResource(Res.string.memory_pick))
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(Res.string.memory_pick), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }

                    if (onMemoryEditorRequested != null) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onMemoryEditorRequested
                        ) {
                            Icon(Icons.Default.Create, stringResource(Res.string.memory_editor))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.memory_editor), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }

                    AnimatedContent(
                        targetState = memoryUploadProgress to isSubmittingMemory,
                        modifier = Modifier.fillMaxWidth(),
                    ) { (progress, submittingMemory) ->
                        if (progress != null) {
                            LinearLoadingIndicator(progress)
                        } else if (submittingMemory) {
                            LinearLoadingIndicator(null)
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(top = 8.dp))

                val list = lending.items.groupBy { it.type }
                Text(stringResource(Res.string.lending_details_items_title))
                for ((type, items) in list) {
                    Text("- ${type.displayName}: ${items.size} items")
                }

                if (PlatformOpenFileLogic.isSupported && lending.memoryDocument != null) {
                    HorizontalDivider()

                    OutlinedButton(
                        onClick = {
                            fpm.openFile { lending.fetchDocumentFilePath() }
                        }
                    ) {
                        Text(stringResource(Res.string.management_view_memory))
                    }
                }

                fpm.progress.LinearLoadingIndicator()
            }
        },
        dismissButton = {
            if (lending.status() in listOf(Lending.Status.REQUESTED, Lending.Status.CONFIRMED)) {
                TextButton(
                    enabled = !isSubmittingMemory,
                    onClick = onCancelRequest
                ) { Text(stringResource(Res.string.lending_details_cancel)) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSubmittingMemory,
                onClick = onDismissRequest
            ) { Text(stringResource(Res.string.close)) }
        },
    )
}
