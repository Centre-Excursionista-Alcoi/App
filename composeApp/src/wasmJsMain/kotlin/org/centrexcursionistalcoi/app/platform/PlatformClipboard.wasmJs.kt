package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.platform.ClipEntry

actual object PlatformClipboard {
    actual fun createClipEntry(text: String, label: String?): ClipEntry {
        return ClipEntry.withPlainText(text)
    }
}
