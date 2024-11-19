package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun <Type : DatabaseData> CreationDialog(
    showingCreationDialog: Type?,
    title: StringResource,
    isCreating: Boolean,
    isEnabled: (Type) -> Boolean,
    onCreateRequested: (Type, onCreate: () -> Unit) -> Unit,
    onDismissRequested: () -> Unit,
    content: @Composable ColumnScope.(Type) -> Unit
) {
    showingCreationDialog?.let { data ->
        PlatformDialog(
            onDismissRequest = { if (!isCreating) onDismissRequested() }
        ) {
            BasicText(
                text = stringResource(title),
                style = getPlatformTextStyles().heading,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            )
            content(data)
            PlatformButton(
                text = stringResource(if (data.id == null) Res.string.create else Res.string.update),
                modifier = Modifier.align(Alignment.End).padding(8.dp),
                enabled = !isCreating && isEnabled(data)
            ) {
                onCreateRequested(data, onDismissRequested)
            }
        }
    }
}
