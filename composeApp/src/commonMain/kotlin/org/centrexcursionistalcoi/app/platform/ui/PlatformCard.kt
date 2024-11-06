package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
expect fun PlatformCard(
    title: String,
    modifier: Modifier = Modifier,
    action: Triple<ImageVector, String, () -> Unit>? = null,
    content: @Composable ColumnScope.() -> Unit
)
