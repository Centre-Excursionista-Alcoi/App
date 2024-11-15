package org.centrexcursionistalcoi.app.pages.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ceaapp.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.BuildKonfig
import org.centrexcursionistalcoi.app.platform.PlatformLanguage
import org.centrexcursionistalcoi.app.platform.languages
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformSettingsItem
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsPage() {
    val uriHandler = LocalUriHandler.current

    if (PlatformLanguage.isLanguageFetchSupported) {
        val language = PlatformLanguage.getSelectedLanguage()
        var showingLanguageChangeDialog by remember { mutableStateOf(false) }

        if (showingLanguageChangeDialog) {
            PlatformDialog(
                onDismissRequest = { showingLanguageChangeDialog = false }
            ) {
                BasicText(
                    text = stringResource(Res.string.settings_language),
                    style = getPlatformTextStyles().heading,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                )
                for (lang in languages) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                PlatformLanguage.changeAppLanguage(lang)
                                showingLanguageChangeDialog = false
                            }
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        BasicText(
                            text = PlatformLanguage.localizedNameForTag(lang),
                            style = getPlatformTextStyles().label,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        PlatformSettingsItem(
            title = stringResource(Res.string.settings_language),
            summary = language?.let(PlatformLanguage::localizedNameForTag)
                ?: stringResource(Res.string.settings_language_default),
            icon = Icons.Default.Language,
            modifier = Modifier.fillMaxWidth(),
            onClick = if (PlatformLanguage.isLanguageChangeSupported) {
                { showingLanguageChangeDialog = true }
            } else {
                null
            }
        )

        HorizontalDivider()
    }

    PlatformSettingsItem(
        title = stringResource(Res.string.settings_version),
        summary = BuildKonfig.VERSION,
        icon = Icons.Default.Title,
        modifier = Modifier.fillMaxWidth()
    ) { uriHandler.openUri("https://github.com/Centre-Excursionista-Alcoi/App/releases/tag/${BuildKonfig.VERSION}") }

    HorizontalDivider()

    PlatformSettingsItem(
        title = stringResource(Res.string.settings_website),
        summary = stringResource(Res.string.tap_to_open),
        icon = Icons.Default.Web,
        modifier = Modifier.fillMaxWidth()
    ) { uriHandler.openUri("https://centrexcursionistalcoi.org/") }
    PlatformSettingsItem(
        title = stringResource(Res.string.settings_source_code),
        summary = stringResource(Res.string.tap_to_open),
        icon = Icons.Default.Code,
        modifier = Modifier.fillMaxWidth()
    ) { uriHandler.openUri("https://github.com/Centre-Excursionista-Alcoi/App/") }
    PlatformSettingsItem(
        title = stringResource(Res.string.settings_server_status),
        summary = stringResource(Res.string.tap_to_open),
        icon = Icons.Default.Dns,
        modifier = Modifier.fillMaxWidth()
    ) { uriHandler.openUri("https://status.escalaralcoiaicomtat.org/status/cea/") }
}
