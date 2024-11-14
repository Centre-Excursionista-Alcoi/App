package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
actual fun PlatformScaffold(
    title: String?,
    actions: List<Triple<ImageVector, String, () -> Unit>>,
    navigationBar: @Composable (() -> Unit)?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.(paddingValues: PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            if (title != null || actions.isNotEmpty()) {
                TopAppBar(
                    title = { title?.let { Text(it) } },
                    actions = {
                        for ((icon, contentDescription, onClick) in actions) {
                            IconButton(onClick) { Icon(icon, contentDescription) }
                        }
                    }
                )
            }
        },
        bottomBar = {
            navigationBar?.invoke()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(it)
        ) {
            content(PaddingValues(0.dp))
        }
    }
}
