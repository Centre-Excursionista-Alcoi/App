package ui.pages.main

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import backend.data.database.InventoryItem
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import ui.reusable.list.InventoryItemCard
import ui.screen.auth.LendingAuthScreen

@Composable
private fun ItemsPlaceholder() {
    val modifier = Modifier
        .widthIn(max = 600.dp)
        .padding(horizontal = 4.dp, vertical = 8.dp)

    LazyVerticalGrid(
        modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth().padding(horizontal = 8.dp),
        columns = GridCells.Adaptive(280.dp)
    ) {
        item { InventoryItemCard(null, modifier = modifier) }
        item { InventoryItemCard(null, modifier = modifier) }
        item { InventoryItemCard(null, modifier = modifier) }
    }
}

@Composable
fun BoxScope.LendingPage(
    items: List<InventoryItem>?,
    lendingAuth: Boolean?,
    isManager: Boolean,
    onIconUpdateRequested: (item: InventoryItem, newIcon: String?) -> Job,
    onDisplayNameUpdateRequested: (item: InventoryItem, displayName: String) -> Job
) {
    val navigator = LocalNavigator.currentOrThrow

    if (items != null && lendingAuth != null) {
        if (lendingAuth) {
            LazyVerticalGrid(
                modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth().padding(horizontal = 8.dp),
                columns = GridCells.Adaptive(280.dp)
            ) {
                items(items) { item ->
                    InventoryItemCard(
                        item,
                        isManager = isManager,
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        onIconUpdateRequested = { onIconUpdateRequested(item, it) },
                        onDisplayNameUpdateRequested = { onDisplayNameUpdateRequested(item, it) }
                    )
                }
            }
        } else {
            OutlinedCard(
                modifier = Modifier.widthIn(max = 400.dp).align(Alignment.Center)
            ) {
                Text(
                    text = stringResource(Res.string.missing_authorization_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = stringResource(Res.string.missing_authorization_lending),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                TextButton(
                    onClick = { navigator.push(LendingAuthScreen()) },
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp)
                ) {
                    Text(stringResource(Res.string.access_form))
                }
            }
        }
    } else {
        ItemsPlaceholder()
    }
}
