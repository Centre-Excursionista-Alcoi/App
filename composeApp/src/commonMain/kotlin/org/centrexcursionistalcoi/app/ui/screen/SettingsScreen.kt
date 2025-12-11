package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cea_app.composeapp.generated.resources.*
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import kotlinx.coroutines.Job
import org.centrexcursionistalcoi.app.push.PlatformSSEConfiguration
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.centrexcursionistalcoi.app.storage.*
import org.centrexcursionistalcoi.app.ui.dialog.RemoveAccountDialog
import org.centrexcursionistalcoi.app.ui.icons.materialsymbols.*
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsCategory
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsOptionsRow
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsRow
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsSwitchRow
import org.centrexcursionistalcoi.app.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class Language(val code: String, val displayName: String, val flag: DrawableResource)

private val availableLanguages = listOf(
    Language("en", "English", Res.drawable.flag_en),
    Language("ca", "Català", Res.drawable.flag_ca),
    Language("es", "Castellano", Res.drawable.flag_es),
)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSettingsApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onDeleteAccount: () -> Unit,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(onDeleteAccount) },
) {
    SettingsScreen(viewModel::deleteAccount, onBack)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSettingsApi::class)
@Composable
private fun SettingsScreen(onAccountDeleteRequest: () -> Job, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = { Text(stringResource(Res.string.settings_title)) }
            )
        }
    ) { paddingValues ->
        LazyColumnWidthWrapper(
            modifier = Modifier.padding(paddingValues).fillMaxWidth()
        ) {
            item(key = "general_category", contentType = "category") {
                SettingsCategory(
                    text = stringResource(Res.string.settings_category_general)
                )
            }
            item(key = "language_option", contentType = "option") {
                SettingsOptionsRow(
                    title = stringResource(Res.string.settings_language),
                    options = availableLanguages,
                    icon = MaterialSymbols.Language,
                    onOptionSelected = { (lang) ->
                        settings.putString(SETTINGS_LANGUAGE, lang)
                        LocaleUpdater.updateLocale(lang)
                    },
                    optionLeadingContent = {
                        Image(
                            painter = painterResource(it.flag),
                            contentDescription = it.displayName,
                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp))
                        )
                    },
                    key = { it.code },
                    toString = { it.displayName },
                )
            }

            if (PlatformSSEConfiguration.enableSSE) {
                item(key = "push_category", contentType = "category") {
                    SettingsCategory(
                        text = stringResource(Res.string.settings_category_push)
                    )
                }
                item(key = "push_sse_connected", contentType = "info") {
                    val isConnected by SSENotificationsListener.isConnected.collectAsState()
                    val sseError by SSENotificationsListener.sseException.collectAsState()
                    SettingsRow(
                        title = stringResource(Res.string.settings_push_connection_title),
                        summary = if (isConnected) {
                            stringResource(Res.string.settings_push_connection_message_connected)
                        } else {
                            stringResource(Res.string.settings_push_connection_message_disconnected)
                        } + (sseError?.message?.let { "\n$it" } ?: ""),
                        onClick = if (sseError != null) { { SSENotificationsListener.startListening() } } else null
                    )
                }
            }

            item(key = "privacy_category", contentType = "category") {
                SettingsCategory(
                    text = stringResource(Res.string.settings_category_privacy)
                )
            }
            item(key = "report_errors", contentType = "option") {
                val checked by settings.getBooleanStateFlow(scope, SETTINGS_PRIVACY_ERRORS, true).collectAsState()
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_report_errors_title),
                    summary = stringResource(Res.string.settings_report_errors_summary),
                    checked = checked,
                    onCheckedChange = { settings.putBoolean(SETTINGS_PRIVACY_ERRORS, it) },
                )
            }
            item(key = "report_analytics", contentType = "option") {
                val checked by settings.getBooleanStateFlow(scope, SETTINGS_PRIVACY_ANALYTICS, true).collectAsState()
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_report_analytics_title),
                    summary = stringResource(Res.string.settings_report_analytics_summary),
                    checked = checked,
                    onCheckedChange = { settings.putBoolean(SETTINGS_PRIVACY_ANALYTICS, it) },
                )
            }
            item(key = "report_session_replay", contentType = "option") {
                val checked by settings.getBooleanStateFlow(scope, SETTINGS_PRIVACY_SESSION_REPLAY, true).collectAsState()
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_report_session_title),
                    summary = stringResource(Res.string.settings_report_session_summary),
                    checked = checked,
                    onCheckedChange = { settings.putBoolean(SETTINGS_PRIVACY_SESSION_REPLAY, it) },
                )
            }

            item(key = "remove_account_spacer", contentType = "spacer") { Spacer(Modifier.height(16.dp)) }
            item(key = "remove_account", contentType = "option") {
                var showingDialog by remember { mutableStateOf(false) }
                if (showingDialog) {
                    RemoveAccountDialog(
                        onConfirm = onAccountDeleteRequest,
                        onDismissRequest = { showingDialog = false },
                    )
                }

                SettingsRow(
                    icon = MaterialSymbols.Warning,
                    title = stringResource(Res.string.settings_remove_account_title),
                    summary = stringResource(Res.string.settings_remove_account_summary),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        headlineColor = MaterialTheme.colorScheme.onErrorContainer,
                        leadingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                        trailingIconColor = MaterialTheme.colorScheme.onErrorContainer,
                        supportingColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { showingDialog = true },
                )
            }

            item(key = "credits") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp).padding(horizontal = 16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Image(painterResource(Res.drawable.arnyminerz), null, modifier = Modifier.size(64.dp).padding(8.dp))

                        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                text = stringResource(Res.string.settings_dev_credit_title, "Arnau Mora"),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = stringResource(Res.string.settings_dev_credit_message, "Arnau Mora Gras"),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 8.dp)) {
                        val uriHandler = LocalUriHandler.current
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { uriHandler.openUri("https://arnyminerz.com") },
                        ) {
                            Icon(MaterialSymbols.Web, null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(Res.string.settings_dev_credit_website))
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { uriHandler.openUri("mailto:arnyminerz@proton.me") },
                        ) {
                            Icon(MaterialSymbols.Mail, null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(Res.string.settings_dev_credit_email))
                        }
                    }
                }
            }
        }
    }
}
