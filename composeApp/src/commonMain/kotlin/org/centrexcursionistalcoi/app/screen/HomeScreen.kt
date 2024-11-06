package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Doorbell
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.centrexcursionistalcoi.app.composition.AccountStateNavigator
import org.centrexcursionistalcoi.app.platform.ui.PlatformNavigationBar
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel

object HomeScreen : Screen<Home, HomeViewModel>(::HomeViewModel) {
    @Composable
    override fun Content(viewModel: HomeViewModel) {
        val user by viewModel.userData.collectAsState()

        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState { 3 }

        LaunchedEffect(Unit) {
            viewModel.load()
        }

        AccountStateNavigator(onLoggedOut = Loading)

        PlatformScaffold(
            title = user?.let { "Welcome, ${it.name}" } ?: "",
            actions = listOf(
                Triple(
                    Icons.AutoMirrored.Rounded.Logout,
                    "Logout"
                ) { viewModel.logout() }
            ),
            navigationBar = {
                PlatformNavigationBar(
                    selection = pagerState.currentPage,
                    onSelectionChanged = { scope.launch { pagerState.animateScrollToPage(it) } },
                    items = listOf(
                        Icons.Default.Dashboard to "Dashboard",
                        Icons.Default.Add to "Reserve",
                        Icons.Default.Doorbell to "Notifications"
                    )
                )
            }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().padding(paddingValues)
            ) {

            }
        }
    }
}
