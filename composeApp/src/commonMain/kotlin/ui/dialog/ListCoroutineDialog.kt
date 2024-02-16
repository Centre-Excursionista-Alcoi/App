package ui.dialog

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import kotlinx.coroutines.Job

@Composable
fun <Type> ListCoroutineDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onSubmit: (() -> Job)?,
    items: List<Type>?,
    itemContent: @Composable CoroutineDialogContext.(isLoading: Boolean, item: Type) -> Unit
) {
    CoroutineDialog(
        title = title,
        onDismissRequest = onDismissRequest,
        onSubmit = onSubmit
    ) { isLoading ->
        LazyColumn {
            items?.let {
                items(it) { item ->
                    itemContent(isLoading, item)
                }
            } ?: item(key = "loading") {
                CircularProgressIndicator()
            }
        }
    }
}
