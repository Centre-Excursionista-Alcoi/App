package org.centrexcursionistalcoi.app.viewmodel

import androidx.lifecycle.ViewModel
import io.ktor.http.ContentType
import io.ktor.http.fileExtensions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import org.centrexcursionistalcoi.app.data.FileContainer
import org.centrexcursionistalcoi.app.di.DispatcherProvider
import org.centrexcursionistalcoi.app.platform.PlatformOpenFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformSaveFileLogic
import org.centrexcursionistalcoi.app.platform.PlatformShareLogic
import org.centrexcursionistalcoi.app.platform.pickAndSave
import org.centrexcursionistalcoi.app.process.Progress
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class FileProviderModel(
    private val dispatcherProvider: DispatcherProvider,
    private val openFileLogic: PlatformOpenFileLogic,
    private val saveFileLogic: PlatformSaveFileLogic,
    private val shareLogic: PlatformShareLogic
) : ViewModel() {
    private val lock = Mutex()

    val isOpeningFileSupported = openFileLogic.isSupported
    val isSharingFileSupported = shareLogic.isSupported

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

    fun saveFile(
        container: FileContainer,
        suggestedName: String,
        contentType: ContentType = ContentType.Application.Pdf
    ) = launchWithLock(lock) {
        saveFileLogic.pickAndSave(
            container,
            fileUuid = container.files.firstNotNullOf { it.value },
            suggestedName = suggestedName,
            allowedExtensions = contentType.fileExtensions().toSet(),
            progressNotifier = progressNotifier,
        )
        progress.value = null
    }

    fun shareFile(
        contentType: ContentType = ContentType.Application.Pdf,
        pathProvider: suspend (ProgressNotifier) -> String
    ) {
        if (!shareLogic.isSupported) return
        launchWithLock(lock) {
            val path = withContext(dispatcherProvider.io) { pathProvider(progressNotifier) }
            shareLogic.share(path, contentType)
            progress.value = null
        }
    }
}
