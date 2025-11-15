package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cea_app.composeapp.generated.resources.*
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import org.centrexcursionistalcoi.app.push.PlatformSSEConfiguration
import org.centrexcursionistalcoi.app.push.SSENotificationsListener
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ANALYTICS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ERRORS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_SESSION_REPLAY
import org.centrexcursionistalcoi.app.storage.settings
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsCategory
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsOptionsRow
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsRow
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsSwitchRow
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
fun SettingsScreen(onBack: () -> Unit) {
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
                    icon = Icons.Default.Language,
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
                            Icon(Icons.Default.Web, null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(Res.string.settings_dev_credit_website))
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { uriHandler.openUri("mailto:arnyminerz@proton.me") },
                        ) {
                            Icon(Icons.Default.Email, null, modifier = Modifier.padding(end = 8.dp))
                            Text(stringResource(Res.string.settings_dev_credit_email))
                        }
                    }
                }
            }
        }
    }
}
