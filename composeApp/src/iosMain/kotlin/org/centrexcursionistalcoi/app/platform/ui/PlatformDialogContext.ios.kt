package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

actual abstract class PlatformDialogContext : RowScope {

    actual abstract fun dismiss()

    @Composable
    actual fun RowScope.PositiveButton(
        text: String,
        enabled: Boolean,
        onClick: () -> Unit
    ) { }

    @Composable
    actual fun RowScope.NeutralButton(
        text: String,
        enabled: Boolean,
        onClick: () -> Unit
    ) { }

    @Composable
    actual fun RowScope.DestructiveButton(
        text: String,
        enabled: Boolean,
        onClick: () -> Unit
    ) { }
}
