package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.lending_details_item_row
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarEndOutline
import org.centrexcursionistalcoi.app.ui.icons.material.CalendarStartOutline
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.jetbrains.compose.resources.pluralStringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LendingsHistoryDialog(lendings: List<ReferencedLending>, onClick: (ReferencedLending) -> Unit, onDismissRequest: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(lendings) { lending ->
                LendingHistoryItem(lending) { onClick(lending) }
            }
        }
    }
}

@Composable
private fun LendingHistoryItem(lending: ReferencedLending, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = MaterialSymbols.CalendarStartOutline,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = lending.from.toString(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = MaterialSymbols.CalendarEndOutline,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = lending.to.toString(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
        }
        val groupedItems = lending.items.groupBy { it.type }
        Text(
            text = groupedItems.map { (type, items) ->
                pluralStringResource(
                    Res.plurals.lending_details_item_row,
                    items.size,
                    type.displayName,
                    items.size
                )
            }.joinToString("\n"),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp),
        )
    }
}
