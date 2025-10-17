package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.data.Lending
import org.jetbrains.compose.resources.stringResource

@Composable
fun LendingDetailsDialog(
    lending: Lending,
    itemTypes: List<InventoryItemType>,
    onCancelRequest: () -> Unit,
    onDismissRequest: () -> Unit
) {
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
