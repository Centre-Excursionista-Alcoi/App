package org.centrexcursionistalcoi.app.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.login_error_passkey
import cea_app.composeapp.generated.resources.passkey_reminder_not_now
import cea_app.composeapp.generated.resources.passkey_reminder_title
import org.centrexcursionistalcoi.app.ui.reusable.PasskeyExplanationCard
import org.centrexcursionistalcoi.app.viewmodel.PasskeyReminderViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Invites users who sign in with a password to create a passkey, explaining what it is. Only shows up when it
 * should (see [org.centrexcursionistalcoi.app.auth.PasskeyUpgrade.shouldRemind]), and "not now" puts it off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeyReminderSheet(model: PasskeyReminderViewModel = koinViewModel()) {
    val isShowing by model.isShowing.collectAsState()
    val isLoading by model.isLoading.collectAsState()
    val error by model.error.collectAsState()

    if (!isShowing) return

    ModalBottomSheet(onDismissRequest = { model.notNow() }) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            Text(
                text = stringResource(Res.string.passkey_reminder_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            PasskeyExplanationCard(
                isLoading = isLoading,
                onCreatePasskey = { model.createPasskey() },
                modifier = Modifier.fillMaxWidth(),
            )
            error?.let {
                Text(
                    text = stringResource(Res.string.login_error_passkey, it.message ?: it.toString()),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            TextButton(
                enabled = !isLoading,
                onClick = { model.notNow() },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text(stringResource(Res.string.passkey_reminder_not_now)) }
        }
    }
}
