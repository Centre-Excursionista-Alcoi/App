package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import io.ktor.http.ContentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FileProviderModel(private val dispatcherProvider: DispatcherProvider) : ViewModel() {
    private val lock = Mutex()

    private val _progress = MutableStateFlow<Progress?>(null)
    val progress = _progress.asStateFlow()

    private val progressNotifier: ProgressNotifier = { _progress.value = it }

    fun openFile(contentType: ContentType = ContentType.Application.Pdf, pathProvider: suspend (ProgressNotifier) -> String) {
        if (!PlatformOpenFileLogic.isSupported) return
        launchWithLock(lock) {
            val path = withContext(dispatcherProvider.io) { pathProvider(progressNotifier) }
            PlatformOpenFileLogic.open(path, contentType)
            _progress.value = null
        }
    }

    fun shareFile(contentType: ContentType = ContentType.Application.Pdf, pathProvider: suspend (ProgressNotifier) -> String) {
        if (!PlatformShareLogic.isSupported) return
        launchWithLock(lock) {
            val path = withContext(dispatcherProvider.io) { pathProvider(progressNotifier) }
            PlatformShareLogic.share(path, contentType)
            _progress.value = null
        }
    }
}
