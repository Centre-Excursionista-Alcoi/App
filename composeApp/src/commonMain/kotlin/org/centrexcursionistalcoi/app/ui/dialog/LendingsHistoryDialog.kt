package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.lending_details_item_row
import org.centrexcursionistalcoi.app.data.ReferencedLending
import org.jetbrains.compose.resources.pluralStringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LendingsHistoryDialog(lendings: List<ReferencedLending>, onClick: (ReferencedLending) -> Unit, onDismissRequest: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        for (lending in lendings) {
            LendingHistoryItem(lending) { onClick(lending) }
        }
    }
}

@Composable
private fun LendingHistoryItem(lending: ReferencedLending, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        Text(
            text = "${lending.from} → ${lending.to}",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textAlign = TextAlign.Center,
        )
        val groupedItems = lending.items.groupBy { it.type }
        Text(
            text = pluralStringResource(
                Res.plurals.lending_details_item_row,
                lending.items.size,
                lending.items.size,
                groupedItems.map { (type, items) -> "${type.displayName} (${items.size})" }.joinToString()
            ),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        )
        Text(
            text = lending.id.toString(),
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            textAlign = TextAlign.Center,
        )
    }
}
