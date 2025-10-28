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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBuilder
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import java.io.File
import kotlin.math.max
import kotlin.math.round
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.ui.theme.AppTheme

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

    fun Float.round(decimals: Int): Float {
        var multiplier = 1f
        repeat(decimals) { multiplier *= 10 }
        return round(this * multiplier) / multiplier
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun WebView(state: Uuid, codeChallenge: String, onProfileProcessed: () -> Unit) {
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
                AuthCallbackProcessor.processCallbackUrl(url)
                onProfileProcessed()
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
            Window(
                title = "Authentication",
                onCloseRequest = { _state.value = null }
            ) {
                var restartRequired by remember { mutableStateOf(false) }
                var downloading by remember { mutableStateOf(0F) }
                var initialized by remember { mutableStateOf(false) }
                val download: KCEFBuilder.Download = remember { KCEFBuilder.Download.Builder().github().build() }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        KCEF.init(
                            builder = {
                                installDir(File("kcef-bundle"))

                                KCEFBuilder.Download.Builder().github {
                                    release("jbr-release-17.0.10b1087.23")
                                }.buffer(download.bufferSize).build()

                                progress {
                                    onDownloading {
                                        downloading = max(it, 0F)
                                    }
                                    onInitialized {
                                        initialized = true
                                    }
                                }
                                settings {
                                    cachePath = File("cache").absolutePath
                                }
                            },
                            onError = {
                                it?.printStackTrace()
                            },
                            onRestartRequired = {
                                restartRequired = true
                            }
                        )
                    }
                }

                AppTheme {
                    if (restartRequired) {
                        Text(text = "Restart required.")
                    } else {
                        if (initialized) {
                            WebView(state, codeChallenge) {
                                KCEF.disposeBlocking()
                                _state.value = null
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
                                        progress = { downloading / 100 }
                                    )
                                    Text(
                                        text = "Preparing environment (${downloading.round(2)}%)",
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
