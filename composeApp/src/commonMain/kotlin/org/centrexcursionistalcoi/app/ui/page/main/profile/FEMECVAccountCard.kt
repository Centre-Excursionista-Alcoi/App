package org.centrexcursionistalcoi.app.ui.page.main.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import io.ktor.util.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.response.ProfileResponse
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.CloudOff
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.CloudSync
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
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
                Icon(imageVector = MaterialSymbols.CloudOff, contentDescription = stringResource(Res.string.femecv_sync_disable))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.femecv_sync_disable))
            }
        } else {
            OutlinedButton(
                onClick = { showLoginDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = MaterialSymbols.CloudSync, contentDescription = stringResource(Res.string.femecv_sync_enable))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.femecv_sync_enable))
            }
        }
    }
}
