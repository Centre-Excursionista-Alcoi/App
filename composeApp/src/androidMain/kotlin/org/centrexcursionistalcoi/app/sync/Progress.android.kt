package org.centrexcursionistalcoi.app.sync

import androidx.work.Data
import org.centrexcursionistalcoi.app.process.Progress

fun Data.toProgress(): Progress {
    val type = getString(BackgroundJobWorker.PROGRESS_KEY_TYPE) ?: return Progress.Default
    val current = getLong(BackgroundJobWorker.PROGRESS_KEY_CURRENT, -1)
    val total = getLong(BackgroundJobWorker.PROGRESS_KEY_TOTAL, -1).takeIf { it >= 0 }
    val name = getString(BackgroundJobWorker.PROGRESS_KEY_NAME)

    return when (type) {
        Progress.Default::class.simpleName -> Progress.Default
        Progress.Download::class.simpleName -> {
            if (name != null) {
                Progress.NamedDownload(name, current, total)
            } else {
                Progress.Download(current, total)
            }
        }
        Progress.Upload::class.simpleName -> {
            if (name != null) {
                Progress.NamedUpload(name, current, total)
            } else {
                Progress.Upload(current, total)
            }
        }
        Progress.LocalFSRead::class.simpleName -> Progress.LocalFSRead(current, total)
        Progress.LocalDBRead::class.simpleName -> Progress.LocalDBRead
        Progress.LocalDBWrite::class.simpleName -> Progress.LocalDBWrite
        Progress.DataProcessing::class.simpleName -> Progress.DataProcessing
        else -> throw IllegalArgumentException("Unknown progress type: $type")
    }
}
