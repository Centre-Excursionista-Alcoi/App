package ui.pages.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import backend.data.user.Role
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.jetbrains.compose.resources.stringResource
import screenmodel.MainScreenModel
import ui.reusable.list.InventoryItemCard
import ui.reusable.navigation.ScaffoldPage
import ui.screen.InventoryItemScreen
import ui.screen.auth.LendingAuthScreen

@OptIn(ExperimentalFoundationApi::class)
class LendingPage(
    private val model: MainScreenModel
) : ScaffoldPage() {
    override val icon: ImageVector = Icons.Outlined.EditNote

    @Composable
    override fun label(): String = stringResource(Res.string.nav_main_lending)

    @Composable
    private fun ItemsPlaceholder() {
        val modifier = Modifier
            .widthIn(max = 600.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp)

        LazyVerticalGrid(
            modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth().padding(horizontal = 8.dp),
            columns = GridCells.Adaptive(280.dp)
        ) {
            item { InventoryItemCard(null, null, modifier = modifier) }
            item { InventoryItemCard(null, null, modifier = modifier) }
            item { InventoryItemCard(null, null, modifier = modifier) }
        }
    }

    @Composable
    override fun PagerScope.PageContent() {
        val navigator = LocalNavigator.currentOrThrow

        val items by model.items.collectAsState(null)
        val categories by model.categories.collectAsState(null)
        val lendingAuth by model.lendingAuth.collectAsState(null)
        val roles by model.userRoles.collectAsState(null)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            items.takeUnless { lendingAuth == null }?.let {
                if (lendingAuth == true) {
                    LazyVerticalGrid(
                        modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth().padding(horizontal = 8.dp),
                        columns = GridCells.Adaptive(280.dp)
                    ) {
                        items(items ?: emptyList()) { item ->
                            InventoryItemCard(
                                item,
                                categories = categories,
                                isManager = roles?.contains(Role.INVENTORY_MANAGER) == true,
                                modifier = Modifier
                                    .widthIn(max = 600.dp)
                                    .padding(horizontal = 4.dp, vertical = 8.dp),
                                onIconUpdateRequested = { model.updateIcon(item, it) },
                                onDisplayNameUpdateRequested = { model.updateDisplayName(item, it) },
                                onCategoryUpdateRequested = { model.updateCategory(item, it) }
                            ) { navigator.push(InventoryItemScreen(item)) }
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
            } ?: ItemsPlaceholder()
        }
    }
}
