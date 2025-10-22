package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

actual object PlatformClipboard {
    @OptIn(ExperimentalComposeUiApi::class)
    actual fun createClipEntry(text: String, label: String?): ClipEntry {
        return ClipEntry.withPlainText(text)
    }
}
