package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.page.home.LendingPage
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(model: HomeViewModel = viewModel { HomeViewModel() }) {
    val profile by model.profile.collectAsState()

    profile?.let {
        HomeScreenContent(it)
    } ?: LoadingBox()
}

@Composable
private fun HomeScreenContent(profile: ProfileResponse) {
    val pager = rememberPagerState { 2 }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Handle navigation item click */ },
                    label = { Text("Home") },
                    icon = { /* Icon can be added here */ }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Handle navigation item click */ },
                    label = { Text(stringResource(Res.string.nav_lending)) },
                    icon = {
                        Icon(Icons.Default.Receipt, stringResource(Res.string.nav_lending))
                    }
                )
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) { page ->
            Column(modifier = Modifier.fillMaxSize()) {
                when (page) {
                    0 -> Text(
                        text = "Welcome back ${profile.username}!",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp)
                    )
                    1 -> LendingPage(profile)
                }
            }
        }
    }
}
