package org.centrexcursionistalcoi.app.process

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import cea_app.composeapp.generated.resources.*
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Contextual
import org.centrexcursionistalcoi.app.defaultAsyncDispatcher
import org.jetbrains.compose.resources.stringResource

sealed class Progress {
    abstract val label: @Composable () -> String?

    sealed class Transfer(val current: Long, val total: Long?) : Progress() {
        val isIndeterminate: Boolean = total == null || total == 0L

        @Contextual
        @get:FloatRange(from = 0.0, to = 1.0)
        val progress: Float? by lazy { if (total == null || total == 0L) null else (current.toFloat() / total.toFloat()) }

        @Contextual
        @get:FloatRange(from = 0.0, to = 100.0)
        val percentage: Float? by lazy { progress?.times(100f) }
    }
    sealed class NamedTransfer(val name: String, current: Long, total: Long?) : Transfer(current, total)

    class Download(current: Long, total: Long?) : Transfer(current, total) {
        override val label: @Composable (() -> String?) = {
            val percentage = percentage
            if (percentage != null) {
                stringResource(Res.string.progress_download_percentage, percentage)
            } else {
                stringResource(Res.string.progress_download)
            }
        }
    }
    class NamedDownload(name: String, current: Long, total: Long?) : NamedTransfer(name, current, total) {
        override val label: @Composable (() -> String?) = {
            val percentage = percentage
            if (percentage != null) {
                stringResource(Res.string.progress_download_name_percentage, name, percentage)
            } else {
                stringResource(Res.string.progress_download_name, name)
            }
        }
    }
    class Upload(current: Long, total: Long?) : Transfer(current, total) {
        override val label: @Composable (() -> String?) = {
            val percentage = percentage
            if (percentage != null) {
                stringResource(Res.string.progress_upload_percentage, percentage)
            } else {
                stringResource(Res.string.progress_upload)
            }
        }
    }
    class NamedUpload(name: String, current: Long, total: Long?) : NamedTransfer(name, current, total) {
        override val label: @Composable (() -> String?) = {
            val percentage = percentage
            if (percentage != null) {
                stringResource(Res.string.progress_upload_name_percentage, percentage)
            } else {
                stringResource(Res.string.progress_upload_name)
            }
        }
    }

    open class LocalFSRead(current: Long, total: Long?) : Transfer(current, total) {
        override val label: @Composable (() -> String?) = { stringResource(Res.string.progress_local_fs_read) }
    }

    object LocalDBRead : Progress() {
        override val label: @Composable (() -> String?) = { stringResource(Res.string.progress_local_db_read) }
    }
    object LocalDBWrite : Progress() {
        override val label: @Composable (() -> String?) = { stringResource(Res.string.progress_local_db_write) }
    }

    object DataProcessing : Progress() {
        override val label: @Composable (() -> String?) = { stringResource(Res.string.progress_data_processing) }
    }

    companion object {
        fun HttpRequestBuilder.monitorUploadProgress(notifier: ProgressNotifier?, name: String? = null) {
            if (notifier == null) return

            // Set initial progress value
            CoroutineScope(defaultAsyncDispatcher).launch {
                notifier(
                    if (name != null) NamedUpload(name, 0, null)
                    else Upload(0, null)
                )
            }
            // Monitor progress updates
            onUpload { current, total ->
                notifier(
                    if (name != null) NamedUpload(name, current, total)
                    else Upload(current, total)
                )
            }
        }

        fun HttpRequestBuilder.monitorDownloadProgress(notifier: ProgressNotifier?, name: String? = null) {
            if (notifier == null) return

            // Set initial progress value
            CoroutineScope(defaultAsyncDispatcher).launch {
                notifier(
                    if (name != null) NamedDownload(name, 0, null)
                    else Download(0, null)
                )
            }
            // Monitor progress updates
            onDownload { current, total ->
                notifier(
                    if (name != null) NamedDownload(name, current, total)
                    else Download(current, total)
                )
            }
        }
    }
}
