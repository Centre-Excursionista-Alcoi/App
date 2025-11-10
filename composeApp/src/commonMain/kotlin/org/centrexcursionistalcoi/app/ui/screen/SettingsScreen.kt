package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.getBooleanStateFlow
import io.github.sudarshanmhasrup.localina.api.LocaleUpdater
import org.centrexcursionistalcoi.app.storage.SETTINGS_LANGUAGE
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ANALYTICS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_ERRORS
import org.centrexcursionistalcoi.app.storage.SETTINGS_PRIVACY_SESSION_REPLAY
import org.centrexcursionistalcoi.app.storage.settings
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsCategory
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsOptionsRow
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsSwitchRow
import org.jetbrains.compose.resources.stringResource

private val availableLanguages = listOf("en" to "English", "ca" to "Català")

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
                    toString = { it.second },
                )
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
        }
    }
}
