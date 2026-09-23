package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import com.mohamedrejeb.calf.core.ExperimentalCalfApi
import com.mohamedrejeb.calf.share.ShareLauncher
import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.DocumentFileContainer
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.platform.PlatformKmpFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformSaveFileLogic
import org.centrexcursionistalcoi.app.platform.pickAndSave
import org.centrexcursionistalcoi.app.platform.toShareContent
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FileProviderModel(
    private val dispatcherProvider: DispatcherProvider,
    private val openFileLogic: PlatformOpenFileLogic,
    private val saveFileLogic: PlatformSaveFileLogic,
    private val kmpFileLogic: PlatformKmpFileLogic,
) : ViewModel() {
    private val lock = Mutex()

    val isOpeningFileSupported = openFileLogic.isSupported

    val progress: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)

    private val progressNotifier: ProgressNotifier = ProgressNotifier { progress.value = it }

    fun openFile(
        contentType: ContentType = ContentType.Application.Pdf,
        pathProvider: suspend (ProgressNotifier) -> String
    ) {
        if (!openFileLogic.isSupported) return
        launchWithLock(lock) {
            val path = withContext(dispatcherProvider.io) { pathProvider(progressNotifier) }
            openFileLogic.open(path, contentType)
            progress.value = null
        }
    }

    /** @return whether the file was actually saved, or `null` on error -- see [pickAndSave]. */
    fun saveFile(
        container: DocumentFileContainer,
        suggestedName: String,
        contentType: ContentType = ContentType.Application.Pdf
    ): Deferred<Boolean?> = async {
        lock.withLock {
            val saved = saveFileLogic.pickAndSave(
                container,
                suggestedName = suggestedName,
                allowedExtensions = contentType.fileExtensions().toSet(),
                progressNotifier = progressNotifier,
            )
            progress.value = null
            saved
        }
    }

    @ExperimentalCalfApi
    fun shareFile(
        shareLauncher: ShareLauncher,
        contentType: ContentType = ContentType.Application.Pdf,
        pathProvider: suspend (ProgressNotifier) -> String
    ) {
        launchWithLock(lock) {
            val path = withContext(dispatcherProvider.io) { pathProvider(progressNotifier) }
            val file = withContext(dispatcherProvider.io) { kmpFileLogic.kmpFile(path, contentType) }
            shareLauncher.launch(file.toShareContent(contentType))
            progress.value = null
        }
    }
}
