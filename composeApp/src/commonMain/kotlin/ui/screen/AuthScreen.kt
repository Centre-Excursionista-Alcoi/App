package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import screenmodel.AuthScreenModel
import ui.pages.auth.HelpPage
import ui.pages.auth.LoginPage
import ui.pages.auth.RegisterPage
import ui.reusable.PopupErrorCard
import ui.state.ConfirmationStatusWatcher
import ui.state.SessionStatusWatcher

@OptIn(ExperimentalFoundationApi::class)
class AuthScreen : BaseScreen() {
    companion object {
        private const val PAGES = 3

        private const val PAGE_REGISTER = 0
        private const val PAGE_LOGIN = 1
        private const val PAGE_HELP = 2

        /**
         * How long to display error cards in milliseconds.
         */
        private const val ERROR_MILLIS = 3_000L
    }

    private lateinit var model: AuthScreenModel

    @Composable
    override fun Content() {
        super.Content()

        val navigator = LocalNavigator.currentOrThrow

        // Clear stack on load
        LaunchedEffect(Unit) { navigator.clearEvent() }

        model = rememberScreenModel { AuthScreenModel() }

        SessionStatusWatcher()
        ConfirmationStatusWatcher()

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            ErrorDisplay()

            PagerComponent()
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun BoxScope.ErrorDisplay() {
        val errors by model.errors.collectAsState(emptyList())

        LazyColumn(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(errors) { index, error ->
                PopupErrorCard(
                    error,
                    ERROR_MILLIS
                ) { model.dismissError(index) }
            }
        }
    }

    @Composable
    private fun PagerComponent() {
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(initialPage = 1) { PAGES }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.onKeyEvent { event ->
                when {
                    event.key == Key.Escape && event.type == KeyEventType.KeyUp -> {
                        if (pagerState.currentPage != 1) {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                        true
                    }
                    else -> false
                }
            }
        ) { page ->
            val isLoading by model.isLoading.collectAsState(false)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    when (page) {
                        PAGE_REGISTER -> RegisterPage(
                            isLoading = isLoading,
                            onLoginRequested = {
                                scope.launch { pagerState.animateScrollToPage(PAGE_LOGIN) }
                            },
                            onRegisterRequested = model::register
                        )

                        PAGE_LOGIN -> LoginPage(
                            isLoading = isLoading,
                            onLoginRequested = model::login,
                            onLostPassword = {
                                scope.launch { pagerState.animateScrollToPage(PAGE_HELP) }
                            },
                            onRegisterRequested = {
                                scope.launch { pagerState.animateScrollToPage(PAGE_REGISTER) }
                            }
                        )

                        PAGE_HELP -> HelpPage()
                    }
                }
            }
        }
    }
}
