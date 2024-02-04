package ui.pages.main

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import backend.data.database.InventoryItem
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.compose.stringResource
import resources.MR
import ui.reusable.list.InventoryItemCard
import ui.screen.auth.LendingAuthScreen

@Composable
private fun ItemsPlaceholder() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        InventoryItemCard(null, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        InventoryItemCard(null, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
        InventoryItemCard(null, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
    }
}

@Composable
fun BoxScope.LendingPage(items: List<InventoryItem>?, lendingAuth: Boolean?) {
    val navigator = LocalNavigator.currentOrThrow

    if (items != null && lendingAuth != null) {
        if (lendingAuth) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                for (item in items) {
                    InventoryItemCard(
                        item,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                    )
                }
            }
        } else {
            OutlinedCard(
                modifier = Modifier.widthIn(max = 400.dp).align(Alignment.Center)
            ) {
                Text(
                    text = stringResource(MR.strings.missing_authorization_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Text(
                    text = stringResource(MR.strings.missing_authorization_lending),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                TextButton(
                    onClick = { navigator.push(LendingAuthScreen()) },
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp)
                ) {
                    Text(stringResource(MR.strings.access_form))
                }
            }
        }
    } else {
        ItemsPlaceholder()
    }
}

@Composable
private fun NotAuthorizedCard() {

}
