package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.data.DatabaseData
import org.centrexcursionistalcoi.app.database.entity.DatabaseEntity
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun <SType : DatabaseData, Type : DatabaseEntity<SType>> CreationDialog(
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
            onDismissRequest = { if (!isCreating) onDismissRequested() },
            title = stringResource(title),
            actions = {
                PositiveButton(
                    text = stringResource(if (data.id <= 0) Res.string.create else Res.string.update),
                    enabled = !isCreating && isEnabled(data)
                ) {
                    onCreateRequested(data, onDismissRequested)
                }
            }
        ) {
            content(data)
        }
    }
}
