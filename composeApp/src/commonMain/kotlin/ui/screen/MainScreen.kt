package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material3.fade
import com.eygraber.compose.placeholder.material3.placeholder
import com.eygraber.compose.placeholder.material3.shimmer
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import resources.MR
import screenmodel.MainScreenModel
import ui.pages.main.RentalPage
import ui.screen.creator.InventoryItemCreator

@OptIn(ExperimentalFoundationApi::class)
class MainScreen : Screen {
    companion object {
        private const val PAGES = 3

        private const val PAGE_RENTAL = 0
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val pagerState = rememberPagerState { PAGES }

        val model = rememberScreenModel { MainScreenModel() }

        val userLoggedOut by model.userLoggedOut.collectAsState(false)
        val currentUser by model.currentUser.collectAsState(null)
        val items by model.items.collectAsState(null)

        LaunchedEffect(userLoggedOut) {
            snapshotFlow { userLoggedOut }.collect { loggedOut ->
                if (loggedOut) {
                    navigator.push(LoadingScreen)
                }
            }
        }

        Scaffold(
            bottomBar = { BottomNavigationBar(pagerState, currentUser == null) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navigator.push(InventoryItemCreator()) }
                ) { Icon(Icons.Rounded.Add, null) }
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { page ->
                when (page) {
                    PAGE_RENTAL -> RentalPage(items)
                    else -> Text("Page $page")
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(
        pagerState: PagerState,
        isLoading: Boolean
    ) {
        val scope = rememberCoroutineScope()

        @Composable
        fun RowScope.Item(
            index: Int,
            icon: ImageVector,
            contentDescription: String?,
            label: StringResource
        ) {
            NavigationBarItem(
                selected = pagerState.currentPage == index,
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier
                            .placeholder(isLoading, highlight = PlaceholderHighlight.fade())
                    )
                },
                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                label = {
                    Text(
                        text = stringResource(label),
                        modifier = Modifier
                            .placeholder(isLoading, highlight = PlaceholderHighlight.fade())
                    )
                }
            )
        }

        NavigationBar {
            Item(
                index = 0,
                icon = Icons.Outlined.EditNote,
                contentDescription = null,
                label = MR.strings.nav_main_rental
            )
            Item(
                index = 1,
                icon = Icons.Outlined.Person,
                contentDescription = null,
                label = MR.strings.nav_main_profile
            )
            Item(
                index = 2,
                icon = Icons.Outlined.Settings,
                contentDescription = null,
                label = MR.strings.nav_main_settings
            )
        }
    }
}
