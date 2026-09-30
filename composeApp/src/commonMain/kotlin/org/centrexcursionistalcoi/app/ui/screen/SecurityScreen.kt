package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.Res
import cea_app.composeapp.generated.resources.cancel
import cea_app.composeapp.generated.resources.confirm_password
import cea_app.composeapp.generated.resources.login_error_passkey
import cea_app.composeapp.generated.resources.login_error_unknown
import cea_app.composeapp.generated.resources.password
import cea_app.composeapp.generated.resources.register_error_password_not_safe
import cea_app.composeapp.generated.resources.security_confirm_message
import cea_app.composeapp.generated.resources.security_confirm_passkey
import cea_app.composeapp.generated.resources.security_confirm_password
import cea_app.composeapp.generated.resources.security_confirm_title
import cea_app.composeapp.generated.resources.security_error_last_login_method
import cea_app.composeapp.generated.resources.security_error_reauthentication
import cea_app.composeapp.generated.resources.security_new_password
import cea_app.composeapp.generated.resources.security_passkey_add
import cea_app.composeapp.generated.resources.security_passkey_created
import cea_app.composeapp.generated.resources.security_passkey_default_name
import cea_app.composeapp.generated.resources.security_passkey_last_used
import cea_app.composeapp.generated.resources.security_passkey_never_used
import cea_app.composeapp.generated.resources.security_passkey_remove
import cea_app.composeapp.generated.resources.security_passkeys_category
import cea_app.composeapp.generated.resources.security_passkeys_unsupported
import cea_app.composeapp.generated.resources.security_password_category
import cea_app.composeapp.generated.resources.security_password_change
import cea_app.composeapp.generated.resources.security_password_not_set_summary
import cea_app.composeapp.generated.resources.security_password_remove
import cea_app.composeapp.generated.resources.security_password_remove_summary
import cea_app.composeapp.generated.resources.security_password_set
import cea_app.composeapp.generated.resources.security_password_set_summary
import cea_app.composeapp.generated.resources.security_title
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.centrexcursionistalcoi.app.auth.PasskeyException
import org.centrexcursionistalcoi.app.data.PasskeyInfo
import org.centrexcursionistalcoi.app.data.SecurityInfo
import org.centrexcursionistalcoi.app.error.Error
import org.centrexcursionistalcoi.app.exception.ServerException
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Delete
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.MaterialSymbols
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.Passkey
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.PasskeyExplanationCard
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.reusable.form.PasswordFormField
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsCategory
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsRow
import org.centrexcursionistalcoi.app.viewmodel.SecurityChange
import org.centrexcursionistalcoi.app.viewmodel.SecurityViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** How the user signs in: their passkeys and password, which they can add, change and remove. */
@Composable
fun SecurityScreen(
    onBack: () -> Unit,
    model: SecurityViewModel = koinViewModel(),
) {
    val info by model.info.collectAsState()
    val isLoading by model.isLoading.collectAsState()
    val error by model.error.collectAsState()
    val pendingChange by model.pendingChange.collectAsState()

    pendingChange?.let {
        ConfirmIdentityDialog(
            canUsePasskey = model.isPasskeysSupported && info?.passkeys?.isNotEmpty() == true,
            canUsePassword = info?.hasPassword == true,
            isLoading = isLoading,
            onPasskey = model::confirmWithPasskey,
            onPassword = model::confirmWithPassword,
            onDismiss = model::cancelChange,
        )
    }

    SecurityScreen(
        info = info,
        isLoading = isLoading,
        error = error,
        isPasskeysSupported = model.isPasskeysSupported,
        onAddPasskey = model::addPasskey,
        onChange = model::request,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityScreen(
    info: SecurityInfo?,
    isLoading: Boolean,
    error: Throwable?,
    isPasskeysSupported: Boolean,
    onAddPasskey: () -> Unit,
    onChange: (SecurityChange) -> Unit,
    onBack: () -> Unit,
) {
    var settingPassword by remember { mutableStateOf(false) }
    if (settingPassword) {
        SetPasswordDialog(
            onSet = {
                settingPassword = false
                onChange(SecurityChange.SetPassword(it))
            },
            onDismiss = { settingPassword = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = { Text(stringResource(Res.string.security_title)) },
            )
        }
    ) { paddingValues ->
        LazyColumnWidthWrapper(modifier = Modifier.padding(paddingValues).fillMaxWidth()) {
            if (isLoading) {
                item(key = "loading") { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            if (error != null) {
                item(key = "error") {
                    Text(
                        text = error.message(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
            }

            if (info == null) return@LazyColumnWidthWrapper

            item(key = "passkeys_category") {
                SettingsCategory(stringResource(Res.string.security_passkeys_category))
            }
            if (info.passkeys.isEmpty() && isPasskeysSupported) {
                item(key = "passkey_explanation") {
                    PasskeyExplanationCard(
                        isLoading = isLoading,
                        onCreatePasskey = onAddPasskey,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
            }
            items(info.passkeys.size, key = { "passkey_${info.passkeys[it].id}" }) { index ->
                val passkey = info.passkeys[index]
                PasskeyRow(passkey, isLoading, onRemove = { onChange(SecurityChange.RemovePasskey(passkey.id)) })
            }
            if (info.passkeys.isNotEmpty() && isPasskeysSupported) {
                item(key = "passkey_add") {
                    OutlinedButton(
                        enabled = !isLoading,
                        onClick = onAddPasskey,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    ) { Text(stringResource(Res.string.security_passkey_add)) }
                }
            }
            if (!isPasskeysSupported) {
                item(key = "passkeys_unsupported") {
                    Text(
                        text = stringResource(Res.string.security_passkeys_unsupported),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                    )
                }
            }

            item(key = "password_category") {
                SettingsCategory(stringResource(Res.string.security_password_category))
            }
            item(key = "password_status") {
                SettingsRow(
                    title = stringResource(if (info.hasPassword) Res.string.security_password_change else Res.string.security_password_set),
                    summary = stringResource(
                        if (info.hasPassword) Res.string.security_password_set_summary else Res.string.security_password_not_set_summary
                    ),
                    onClick = { if (!isLoading) settingPassword = true },
                )
            }
            if (info.hasPassword && info.passkeys.isNotEmpty()) {
                item(key = "password_remove") {
                    SettingsRow(
                        title = stringResource(Res.string.security_password_remove),
                        summary = stringResource(Res.string.security_password_remove_summary),
                        onClick = { if (!isLoading) onChange(SecurityChange.RemovePassword) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PasskeyRow(passkey: PasskeyInfo, isLoading: Boolean, onRemove: () -> Unit) {
    SettingsRow(
        icon = MaterialSymbols.Passkey,
        title = passkey.name ?: stringResource(Res.string.security_passkey_default_name),
        summary = listOf(
            stringResource(Res.string.security_passkey_created, passkey.createdAt.date()),
            passkey.lastUsedAt?.let { stringResource(Res.string.security_passkey_last_used, it.date()) }
                ?: stringResource(Res.string.security_passkey_never_used),
        ).joinToString("\n"),
        trailingContent = {
            IconButton(enabled = !isLoading, onClick = onRemove) {
                Icon(MaterialSymbols.Delete, stringResource(Res.string.security_passkey_remove))
            }
        },
    )
}

private fun Instant.date(): String = toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

@Composable
private fun Throwable.message(): String = when (this) {
    is ServerException -> when (errorCode) {
        Error.ERROR_REAUTHENTICATION_FAILED -> stringResource(Res.string.security_error_reauthentication)
        Error.ERROR_LAST_LOGIN_METHOD -> stringResource(Res.string.security_error_last_login_method)
        Error.ERROR_PASSWORD_NOT_SAFE_ENOUGH -> stringResource(Res.string.register_error_password_not_safe)
        else -> stringResource(Res.string.login_error_unknown, message ?: toString())
    }
    is PasskeyException -> stringResource(Res.string.login_error_passkey, message ?: toString())
    else -> toString()
}

@Composable
private fun SetPasswordDialog(onSet: (String) -> Unit, onDismiss: () -> Unit) {
    val password = rememberTextFieldState()
    val passwordConfirm = rememberTextFieldState()
    val valid = password.text.isNotBlank() && password.text == passwordConfirm.text

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.security_password_set)) },
        text = {
            Column {
                Text(stringResource(Res.string.register_error_password_not_safe))
                PasswordFormField(
                    state = password,
                    label = stringResource(Res.string.security_new_password),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    semanticsIsNewPassword = true,
                    showNextButton = true,
                )
                PasswordFormField(
                    state = passwordConfirm,
                    label = stringResource(Res.string.confirm_password),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    semanticsIsNewPassword = true,
                    showNextButton = false,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSet(password.text.toString()) }) {
                Text(stringResource(Res.string.security_password_set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

/** Asks the user to confirm it's them, with a passkey or their password, before a sensitive change. */
@Composable
private fun ConfirmIdentityDialog(
    canUsePasskey: Boolean,
    canUsePassword: Boolean,
    isLoading: Boolean,
    onPasskey: () -> Unit,
    onPassword: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val password = rememberTextFieldState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.security_confirm_title)) },
        text = {
            Column {
                Text(stringResource(Res.string.security_confirm_message))
                if (canUsePasskey) {
                    OutlinedButton(
                        enabled = !isLoading,
                        onClick = onPasskey,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) { Text(stringResource(Res.string.security_confirm_passkey)) }
                }
                if (canUsePassword) {
                    PasswordFormField(
                        state = password,
                        label = stringResource(Res.string.password),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = !isLoading,
                        showNextButton = false,
                    )
                }
            }
        },
        confirmButton = {
            if (canUsePassword) {
                TextButton(
                    enabled = !isLoading && password.text.isNotBlank(),
                    onClick = { onPassword(password.text.toString()) },
                ) { Text(stringResource(Res.string.security_confirm_password)) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
