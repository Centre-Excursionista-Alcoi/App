package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
expect fun PlatformScaffold(
    title: String? = null,
    actions: List<Triple<ImageVector, String, () -> Unit>> = emptyList(),
    navigationBar: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.(paddingValues: PaddingValues) -> Unit
)
