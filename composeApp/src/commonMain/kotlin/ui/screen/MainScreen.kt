package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.launch
import resources.MR

@OptIn(ExperimentalFoundationApi::class)
class MainScreen : Screen {
    companion object {
        private const val PAGES = 3
    }

    @Composable
    override fun Content() {
        val pagerState = rememberPagerState { PAGES }

        Scaffold(
            bottomBar = { BottomNavigationBar(pagerState) }
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { page ->
                Text("Page $page")
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(pagerState: PagerState) {
        val scope = rememberCoroutineScope()

        NavigationBar {
            NavigationBarItem(
                selected = pagerState.currentPage == 0,
                icon = { Icon(Icons.Outlined.EditNote, null) },
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                label = { Text(stringResource(MR.strings.nav_main_rental)) }
            )
            NavigationBarItem(
                selected = pagerState.currentPage == 1,
                icon = { Icon(Icons.Outlined.Person, null) },
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                label = { Text(stringResource(MR.strings.nav_main_profile)) }
            )
            NavigationBarItem(
                selected = pagerState.currentPage == 2,
                icon = { Icon(Icons.Outlined.Settings, null) },
                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                label = { Text(stringResource(MR.strings.nav_main_settings)) }
            )
        }
    }
}
