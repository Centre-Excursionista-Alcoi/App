package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingDetailsDialog(
    lending: Lending,
    itemTypes: List<InventoryItemType>,
    onCancelRequest: () -> Unit,
    onMemorySubmitted: (PlatformFile) -> Job,
    onDismissRequest: () -> Unit,
) {
    var isSubmittingMemory by remember { mutableStateOf(false) }
    val filePickLauncher = rememberFilePickerLauncher(FileKitType.File("pdf")) { file ->
        if (file == null) return@rememberFilePickerLauncher
        isSubmittingMemory = true
        onMemorySubmitted(file).invokeOnCompletion { isSubmittingMemory = false }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.lending_details_title)) },
        text = {
            Column {
                Text("Lending ID: ${lending.id}")
                Text("From: ${lending.from}")
                Text("To: ${lending.to}")

                if (lending.isReturned()) {
                    Text("Returned on: ${lending.receivedAt}")
                } else if (lending.isTaken()) {
                    Text("Taken on: ${lending.givenAt}")
                } else if (lending.confirmed) {
                    Text("Status: Confirmed")
                } else {
                    Text("Status: Pending")
                }
                if (lending.memoryPending()) {
                    Text(
                        text = stringResource(resource = Res.string.memory_pending_lending),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    TextButton(
                        enabled = !isSubmittingMemory,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = { filePickLauncher.launch() }
                    ) { Text(stringResource(Res.string.memory_pick)) }
                }

                HorizontalDivider()

                val list = lending.items.groupBy { it.type }
                Text(stringResource(Res.string.lending_details_items_title))
                for ((typeId, items) in list) {
                    val type = itemTypes.find { it.id == typeId }
                    Text("- ${type?.displayName ?: "Unknown"}: ${items.size} items")
                }
            }
        },
        dismissButton = {
            if (!lending.isTaken()) {
                TextButton(
                    onClick = onCancelRequest
                ) { Text(stringResource(Res.string.lending_details_cancel)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest
            ) { Text(stringResource(Res.string.close)) }
        },
    )
}
