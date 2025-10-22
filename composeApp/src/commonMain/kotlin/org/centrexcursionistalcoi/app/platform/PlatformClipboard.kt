package org.centrexcursionistalcoi.app.platform

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

expect object PlatformClipboard {
    fun createClipEntry(text: String, label: String? = null): ClipEntry
}

suspend fun Clipboard.setClipEntry(text: String, label: String? = null) = setClipEntry(PlatformClipboard.createClipEntry(text, label))
