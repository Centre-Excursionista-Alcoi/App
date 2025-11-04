package org.centrexcursionistalcoi.app.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import kotlin.math.round
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.ui.reusable.LoadingBox
import org.centrexcursionistalcoi.app.ui.theme.AppTheme
import org.centrexcursionistalcoi.app.viewmodel.AuthFlowViewModel
import org.jetbrains.compose.resources.painterResource

object AuthFlowWindow {
    private val _state = MutableStateFlow<FlowState?>(null)
    val state = _state.asStateFlow()

    data class FlowState(
        val state: Uuid,
        val codeChallenge: String,
    )

    fun start(state: Uuid, codeChallenge: String) {
        _state.value = FlowState(state, codeChallenge)
    }

    fun close() {
        _state.value = null
    }

    fun Float.round(decimals: Int): Float {
        var multiplier = 1f
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun WebView(
        state: Uuid,
        codeChallenge: String,
        onProtocolRedirection: (Url) -> Unit
    ) {
        val webViewState = rememberWebViewState(
            URLBuilder(BuildKonfig.SERVER_URL)
                .appendPathSegments("login")
                .apply {
                    parameters["redirect_uri"] = BuildKonfig.REDIRECT_URI ?: error("REDIRECT_URI is not set")
                    parameters["state"] = state.toString()
                    parameters["code_challenge"] = codeChallenge
                }
                .buildString()
        )

        LaunchedEffect(webViewState.lastLoadedUrl) {
            val url = webViewState.lastLoadedUrl?.let { Url(it) } ?: return@LaunchedEffect
            if (url.protocol.name == "cea") {
                onProtocolRedirection(url)
            } else {
                Napier.d { "URL (${url.protocol.name}): $url" }
            }
        }

        if (webViewState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (val loadingState = webViewState.loadingState) {
                    is LoadingState.Loading -> CircularWavyProgressIndicator(progress = { loadingState.progress })
                    else -> CircularWavyProgressIndicator()
                }
            }
        }
        val errors = webViewState.errorsForCurrentRequest
        LaunchedEffect(errors) {
            if (errors.isNotEmpty()) {
                // Handle errors appropriately in your app
                Napier.e("WebView errors:\n${errors.joinToString("\n") { "#${it.code}: ${it.description}" }}")
            }
        }

        WebView(
            state = webViewState,
            modifier = Modifier.fillMaxSize()
        )
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    operator fun invoke() {
        val stateFlow by state.collectAsState()
        stateFlow?.let { (state, codeChallenge) ->
            var onCloseRequest: () -> Unit = {}

            Window(
                title = "Authentication",
                icon = painterResource(Res.drawable.icon),
                onCloseRequest = { onCloseRequest() },
            ) {
                val model: AuthFlowViewModel = viewModel { AuthFlowViewModel() }
                LaunchedEffect(model) {
                    onCloseRequest = { model.close() }
                }

                val restartRequired by model.restartRequired.collectAsState()
                val downloadProgress by model.downloadProgress.collectAsState()
                val initialized by model.isInitialized.collectAsState()
                val isLoading by model.isLoading.collectAsState()

                AppTheme {
                    if (restartRequired) {
                        Text(text = "Restart required.")
                    } else if (isLoading) {
                        LoadingBox()
                    } else {
                        if (initialized) {
                            WebView(state, codeChallenge) { url ->
                                model.processUrl(url)
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    CircularWavyProgressIndicator(
                                        progress = { downloadProgress / 100 }
                                    )
                                    Text(
                                        text = "Preparing environment (${downloadProgress.round(2)}%)",
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
