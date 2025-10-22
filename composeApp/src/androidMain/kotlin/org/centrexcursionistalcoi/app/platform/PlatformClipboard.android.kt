package org.centrexcursionistalcoi.app.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual object PlatformClipboard {
    actual fun createClipEntry(text: String, label: String?): ClipEntry {
        return ClipEntry(
            ClipData.newPlainText(label, text)
        )
    }
}
