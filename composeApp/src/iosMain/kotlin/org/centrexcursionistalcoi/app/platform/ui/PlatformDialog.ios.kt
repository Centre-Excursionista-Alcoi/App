package org.centrexcursionistalcoi.app.platform.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import io.github.alexzhirkevich.cupertino.CupertinoAlertDialog
import io.github.alexzhirkevich.cupertino.CupertinoText
import io.github.alexzhirkevich.cupertino.ExperimentalCupertinoApi

@OptIn(ExperimentalCupertinoApi::class)
@Composable
actual fun PlatformDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties,
    title: String?,
    actions: @Composable PlatformDialogContext.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    CupertinoAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
        title = { title?.let { CupertinoText(it) } },
        message = {
            Column { content() }
        },
        buttons = {

        }
    )
}
