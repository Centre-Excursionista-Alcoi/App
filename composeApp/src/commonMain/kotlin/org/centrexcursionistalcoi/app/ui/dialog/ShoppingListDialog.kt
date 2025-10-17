package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.InventoryItemType
import org.centrexcursionistalcoi.app.typing.ShoppingList
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ShoppingListDialog(
    shoppingList: ShoppingList,
    inventoryItemTypes: List<InventoryItemType>?,
    onContinue: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Text(
            text = stringResource(Res.string.shopping_list_summary),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            textAlign = TextAlign.Center,
        )

        for ((typeId, amount) in shoppingList) {
            val itemType = inventoryItemTypes?.firstOrNull { it.id == typeId } ?: continue
            ListItem(
                headlineContent = { Text(itemType.displayName) },
                trailingContent = {
                    Text("x$amount")
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                modifier = Modifier.padding(end = 12.dp),
                onClick = onContinue,
            ) {
                Text(stringResource(Res.string.shopping_list_confirm))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
