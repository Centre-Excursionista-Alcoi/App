package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

@Composable
expect fun PlatformScaffold(
    title: String? = null,
    actions: List<Action> = emptyList(),
    navigationBar: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.(paddingValues: PaddingValues) -> Unit
)
