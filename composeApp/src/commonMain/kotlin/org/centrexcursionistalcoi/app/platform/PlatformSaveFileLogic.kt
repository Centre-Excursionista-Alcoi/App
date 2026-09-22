package org.centrexcursionistalcoi.app.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.sink
import io.ktor.utils.io.core.writeFully
import kotlinx.io.Buffer
import kotlinx.io.Source
import org.centrexcursionistalcoi.app.data.DocumentFileContainer
import org.centrexcursionistalcoi.app.data.readFile
import org.centrexcursionistalcoi.app.process.ProgressNotifier
import org.koin.core.annotation.Singleton

@Singleton
class PlatformSaveFileLogic {
    fun save(output: PlatformFile, data: Source, override: Boolean = true) {
        if (output.exists() && !override) {
            throw IllegalStateException("File already exists and override is set to false")
        }

        output.sink().use { sink ->
            data.transferTo(sink)
        }
    }
}

suspend fun PlatformSaveFileLogic.pickAndSave(
    container: DocumentFileContainer,
    suggestedName: String = container.documentFile.toString(),
    allowedExtensions: Set<String> = setOf(),
    defaultExtension: String? = allowedExtensions.firstOrNull(),
    progressNotifier: ProgressNotifier? = null
) {
    val result = FileKit.openFileSaver(
        suggestedName = suggestedName,
        defaultExtension = defaultExtension,
        allowedExtensions = allowedExtensions.takeIf { it.isNotEmpty() }
    ) ?: return

    val bytes = container.readFile(progressNotifier)
    val buffer = Buffer()
    buffer.writeFully(bytes)
    buffer.use { data ->
        save(result, data)
    }
}
