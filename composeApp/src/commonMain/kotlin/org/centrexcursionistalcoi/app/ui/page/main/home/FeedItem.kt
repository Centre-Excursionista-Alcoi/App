package org.centrexcursionistalcoi.app.ui.page.main.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun FeedItem(
    icon: ImageVector,
    title: String,
    dateString: String,
    content: String?,
    dialogContent: @Composable ColumnScope.() -> Unit,
) {
    var showingDialog by remember { mutableStateOf(false) }
    if (showingDialog) {
        val sheetState = rememberModalBottomSheetState()

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showingDialog = false },
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(state = rememberScrollState(), enabled = sheetState.currentValue == SheetValue.Expanded)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                dialogContent()
            }
        }
    }

    OutlinedCard(
        modifier = Modifier.padding(8.dp),
        onClick = { showingDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Text(
            text = dateString,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 8.dp),
        )
        if (content != null) {
            if (content.length > 150) {
                Text(
                    text = "${content.take(150)}...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 8.dp),
                )
            } else {
                Markdown(
                    content,
                    modifier = Modifier.padding(bottom = 8.dp).padding(horizontal = 8.dp),
                )
            }
        }
    }
}
