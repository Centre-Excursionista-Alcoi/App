package org.centrexcursionistalcoi.app.platform.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
actual fun PlatformScaffold(
    title: String?,
    actions: List<Action>,
    navigationBar: @Composable (() -> Unit)?,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.(paddingValues: PaddingValues) -> Unit
) {
    BackHandler(enabled = onBack != null) {
        onBack?.invoke()
    }

    Scaffold(
        topBar = {
            if (title != null || actions.isNotEmpty()) {
                TopAppBar(
                    title = { title?.let { Text(it) } },
                    navigationIcon = {
                        if (onBack != null) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = stringResource(Res.string.back)
                                )
                            }
                        }
                    },
                    actions = {
                        for (action in actions) {
                            if (action.badge != null) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text(action.badge) }
                                    }
                                ) {
                                    IconButton(
                                        enabled = action.enabled,
                                        onClick = action.onClick
                                    ) { Icon(action.icon, action.label) }
                                }
                            } else {
                                IconButton(
                                    enabled = action.enabled,
                                    onClick = action.onClick
                                ) { Icon(action.icon, action.label) }
                            }
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
