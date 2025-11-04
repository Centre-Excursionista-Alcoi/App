package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import dev.datlag.kcef.KCEF
import dev.datlag.kcef.KCEFBuilder
import io.ktor.http.Url
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.centrexcursionistalcoi.app.auth.AuthCallbackProcessor
import org.centrexcursionistalcoi.app.auth.AuthFlowWindow

class AuthFlowViewModel : ViewModel() {
    private val _restartRequired = MutableStateFlow(false)
    val restartRequired get() = _restartRequired.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress get() = _downloadProgress.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized get() = _isInitialized.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading get() = _isLoading.asStateFlow()

    init {
        launch {
            val download = KCEFBuilder.Download.Builder().github().build()

            KCEF.init(
                builder = {
                    installDir(File("kcef-bundle"))

                    KCEFBuilder.Download.Builder().github {
                        release("jbr-release-17.0.10b1087.23")
                    }.buffer(download.bufferSize).build()

                    progress {
                        onDownloading {
                            _downloadProgress.value = max(it, 0F)
                        }
                        onInitialized {
                            _isInitialized.value = true
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
                    _restartRequired.value = true
                }
            )
        }
    }

    fun processUrl(url: Url) = launch {
        try {
            _isLoading.value = true
            AuthCallbackProcessor.processCallbackUrl(url)
            KCEF.dispose()

            AuthFlowWindow.close()
        } finally {
            _isLoading.value = false
        }
    }

    fun close() = launch {
        try {
            _isLoading.value = true

            KCEF.dispose()

            AuthFlowWindow.close()
        } finally {
            _isLoading.value = false
        }
    }
}
