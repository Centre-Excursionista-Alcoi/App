package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties

@Composable
expect fun PlatformDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    title: String? = null,
    actions: @Composable PlatformDialogContext.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
)
