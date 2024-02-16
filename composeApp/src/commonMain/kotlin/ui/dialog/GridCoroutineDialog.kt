package ui.dialog

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job

@Composable
fun <Type> GridCoroutineDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onSubmit: (() -> Job)?,
    items: List<Type>,
    columns: GridCells = GridCells.FixedSize(48.dp),
    itemContent: @Composable CoroutineDialogContext.(isLoading: Boolean, item: Type) -> Unit
) {
    CoroutineDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        onSubmit = onSubmit
    ) { isLoading ->
        LazyVerticalGrid(
            columns = columns
        ) {
            items(items) { item ->
                itemContent(isLoading, item)
            }
        }
    }
}
