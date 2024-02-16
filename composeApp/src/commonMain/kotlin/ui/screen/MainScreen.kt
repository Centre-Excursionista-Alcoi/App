package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import backend.data.user.Role
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import screenmodel.MainScreenModel
import ui.pages.main.LendingPage
import ui.pages.main.ProfilePage
import ui.pages.main.SettingsPage
import ui.reusable.AdaptiveScaffold
import ui.screen.creator.InventoryItemCreator

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class MainScreen : BaseScreen() {
    lateinit var model: MainScreenModel
        private set

    @Composable
    override fun ScreenContent() {
        val navigator = LocalNavigator.currentOrThrow

        model = rememberScreenModel { MainScreenModel() }

        val userLoggedOut by model.userLoggedOut.collectAsState(false)
        val currentUser by model.currentUser.collectAsState(null)
        val roles by model.userRoles.collectAsState(null)

        LaunchedEffect(userLoggedOut) {
            snapshotFlow { userLoggedOut }.collect { loggedOut ->
                if (loggedOut) {
                    navigator.push(LoadingScreen())
                }
            }
        }

        AdaptiveScaffold(
            pages = listOf(
                LendingPage(model), ProfilePage(), SettingsPage()
            ),
            floatingActionButton = {
                if (roles?.contains(Role.INVENTORY_MANAGER) == true) {
                    FloatingActionButton(
                        onClick = { navigator.push(InventoryItemCreator()) }
                    ) { Icon(Icons.Rounded.Add, null) }
                }
            },
            loadingItems = currentUser == null
        )
    }
}
