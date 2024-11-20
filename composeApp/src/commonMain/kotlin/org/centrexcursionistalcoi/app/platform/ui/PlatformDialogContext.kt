package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable

expect abstract class PlatformDialogContext : RowScope {
    abstract fun dismiss()

    @Composable
    fun RowScope.PositiveButton(text: String, enabled: Boolean = true, onClick: () -> Unit)

    @Composable
    fun RowScope.NeutralButton(text: String, enabled: Boolean = true, onClick: () -> Unit)

    @Composable
    fun RowScope.DestructiveButton(text: String, enabled: Boolean = true, onClick: () -> Unit)
}
