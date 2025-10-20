package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.ktor.http.ContentType
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.centrexcursionistalcoi.app.data.documentFilePath
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingDetailsDialog(
    lending: Lending,
    itemTypes: List<InventoryItemType>,
    onCancelRequest: () -> Unit,
    memoryUploadProgress: Pair<Long, Long>?,
    onMemorySubmitted: ((PlatformFile) -> Job)?,
    onDismissRequest: () -> Unit,
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
                    else -> Text("Returned on: ${lending.receivedAt}")
                }
                if (status == Lending.Status.RETURNED) {
                    Text(
                        text = stringResource(resource = Res.string.memory_pending_lending),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    TextButton(
                        enabled = !isSubmittingMemory,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { filePickLauncher.launch() }
                    ) { Text(stringResource(Res.string.memory_pick)) }
                    AnimatedContent(
                        targetState = memoryUploadProgress to isSubmittingMemory,
                        modifier = Modifier.fillMaxWidth(),
                    ) { (progress, submittingMemory) ->
                        if (progress != null) {
                            val (uploaded, total) = progress
                            val percentage = uploaded.toDouble() / total.toDouble()
                            LinearProgressIndicator(
                                progress = { percentage.toFloat() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else if (submittingMemory) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(top = 8.dp))

                val list = lending.items.groupBy { it.type }
                Text(stringResource(Res.string.lending_details_items_title))
                for ((typeId, items) in list) {
                    val type = itemTypes.find { it.id == typeId }
                    Text("- ${type?.displayName ?: "Unknown"}: ${items.size} items")
                }

                if (PlatformOpenFileLogic.supported && lending.memoryDocument != null) {
                    HorizontalDivider()

                    OutlinedButton(
                        onClick = {
                            val path = lending.documentFilePath()
                            PlatformOpenFileLogic.open(path, ContentType.Application.Pdf)
                        }
                    ) {
                        Text(stringResource(Res.string.management_view_memory))
                    }
                }
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
