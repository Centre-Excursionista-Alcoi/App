package org.centrexcursionistalcoi.app.ui.page.home.management

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.data.UserData
import org.centrexcursionistalcoi.app.ui.reusable.TooltipIconButton
import org.centrexcursionistalcoi.app.ui.reusable.form.ReadOnlyFormField
import org.jetbrains.compose.resources.stringResource

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UsersListView(
    windowSizeClass: WindowSizeClass,
    users: List<UserData>?,
    onPromote: (UserData) -> Job,
) {
    var promotingUser by remember { mutableStateOf<UserData?>(null) }
    promotingUser?.let { user ->
        var isPromoting by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!isPromoting) promotingUser = null },
            title = { Text(stringResource(Res.string.management_promote_user_title)) },
            text = { Text(stringResource(Res.string.management_promote_user_confirmation, user.fullName)) },
            confirmButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = {
                        isPromoting = true
                        onPromote(user).invokeOnCompletion {
                            isPromoting = false
                            promotingUser = null
                        }
                    }
                ) { Text(stringResource(Res.string.management_promote_user)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isPromoting,
                    onClick = { if (!isPromoting) promotingUser = null }
                ) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    ListView(
        windowSizeClass = windowSizeClass,
        items = users,
        itemIdProvider = { it.id },
        itemDisplayName = { it.fullName },
        emptyItemsText = stringResource(Res.string.management_no_users),
        itemToolbarActions = {
            TooltipIconButton(
                imageVector = Icons.Default.AddModerator,
                tooltip = stringResource(Res.string.management_promote_user),
                positioning = TooltipAnchorPosition.Left,
                onClick = { promotingUser = it },
            )
        },
        // users cannot be created or edited
        isCreatingSupported = false,
        editItemContent = null,
    ) { user ->
        ReadOnlyFormField(
            value = user.fullName,
            label = stringResource(Res.string.personal_info_full_name),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
