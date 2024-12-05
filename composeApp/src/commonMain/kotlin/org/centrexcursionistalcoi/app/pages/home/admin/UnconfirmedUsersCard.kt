package org.centrexcursionistalcoi.app.pages.home.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.database.entity.admin.User
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.platform.ui.PlatformCard
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.jetbrains.compose.resources.stringResource

@Composable
fun UnconfirmedUsersCard(
    unconfirmedUsers: List<User>,
    isConfirming: Boolean,
    onConfirmRequested: (User, onComplete: () -> Unit) -> Unit,
    onDeleteRequested: (User, onComplete: () -> Unit) -> Unit,
    confirmingUser: User?,
    onConfirmingUserRequested: (User) -> Unit,
    onConfirmingUserCancelled: () -> Unit
) {
    confirmingUser?.let { user ->
        PlatformDialog(
            onDismissRequest = onConfirmingUserCancelled
        ) {
            AppText(
                text = stringResource(Res.string.unconfirmed_users_message),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            AppText(
                text = stringResource(Res.string.unconfirmed_users_full_name, user.name + " " + user.familyName),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )
            AppText(
                text = stringResource(Res.string.unconfirmed_users_email, user.email),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )
            AppText(
                text = stringResource(Res.string.unconfirmed_users_email, user.email),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )
            AppText(
                text = stringResource(Res.string.unconfirmed_users_phone, user.phone),
                style = getPlatformTextStyles().label,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PlatformButton(
                    text = stringResource(Res.string.delete),
                    modifier = Modifier.padding(end = 8.dp).padding(vertical = 8.dp),
                    enabled = !isConfirming
                ) {
                    onDeleteRequested(user, onConfirmingUserCancelled)
                }
                PlatformButton(
                    text = stringResource(Res.string.confirm),
                    modifier = Modifier.padding(end = 8.dp).padding(vertical = 8.dp),
                    enabled = !isConfirming
                ) {
                    onConfirmRequested(user, onConfirmingUserCancelled)
                }
            }
        }
    }

    PlatformCard(
        title = stringResource(Res.string.unconfirmed_users_title),
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        for (user in unconfirmedUsers) {
            PlatformCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable { onConfirmingUserRequested(user) }
            ) {
                AppText(
                    text = user.email,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .padding(top = 8.dp),
                    style = getPlatformTextStyles().label.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
