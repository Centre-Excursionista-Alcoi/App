package org.centrexcursionistalcoi.app.ui.page.home.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.ktor.util.toCharArray
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.reusable.InformationCard
import org.centrexcursionistalcoi.app.ui.reusable.form.PasswordFormField
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun FEMECVAccountCard(
    profile: ProfileResponse,
    onConnectRequested: (username: String, password: CharArray) -> Deferred<Throwable?>,
    onDisconnectRequested: () -> Job,
) {
    var showLoginDialog by remember { mutableStateOf(false) }
    if (showLoginDialog) {
        var username by remember { mutableStateOf("") }
        val password = rememberTextFieldState()
        var error by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isLoading) showLoginDialog = false },
            title = { Text(stringResource(Res.string.femecv_sync_enable)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painterResource(Res.drawable.logo_femecv),
                        contentDescription = stringResource(Res.string.femecv_logo),
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(stringResource(Res.string.femecv_username)) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !isLoading,
                    )
                    PasswordFormField(
                        state = password,
                        label = stringResource(Res.string.femecv_password),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    Text(
                        text = stringResource(Res.string.femecv_notice),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isLoading,
                    onClick = { showLoginDialog = false }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isLoading && username.isNotBlank() && password.text.isNotBlank(),
                    onClick = {
                        isLoading = true
                        error = null
                        val job = onConnectRequested(username, password.text.toString().toCharArray())
                        job.invokeOnCompletion {
                            val err = job.getCompleted()
                            if (err == null) showLoginDialog = false
                            else error = err.message
                            isLoading = false
                        }
                    }
                ) {
                    Text(stringResource(Res.string.femecv_sync_login))
                }
            }
        )
    }

    var isDisconnecting by remember { mutableStateOf(false) }

    InformationCard(
        title = stringResource(Res.string.femecv_sync_title),
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    ) {
        if (profile.femecvSyncEnabled) {
            Text(
                text = stringResource(Res.string.femecv_sync_connected),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedButton(
                onClick = {
                    isDisconnecting = true
                    onDisconnectRequested().invokeOnCompletion {
                        isDisconnecting = false
                    }
                },
            ) {
                Icon(imageVector = Icons.Default.CloudOff, contentDescription = stringResource(Res.string.femecv_sync_disable))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.femecv_sync_disable))
            }
        } else {
            OutlinedButton(
                onClick = { showLoginDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Default.SyncAlt, contentDescription = stringResource(Res.string.femecv_sync_enable))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.femecv_sync_enable))
            }
        }
    }
}
