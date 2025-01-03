package org.centrexcursionistalcoi.app.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import org.centrexcursionistalcoi.app.component.AppText
import org.centrexcursionistalcoi.app.composition.LocalNavController
import org.centrexcursionistalcoi.app.pages.home.PlatformSettings
import org.centrexcursionistalcoi.app.platform.PlatformLanguage
import org.centrexcursionistalcoi.app.platform.languages
import org.centrexcursionistalcoi.app.platform.ui.PlatformDialog
import org.centrexcursionistalcoi.app.platform.ui.PlatformScaffold
import org.centrexcursionistalcoi.app.platform.ui.PlatformSettingsItem
import org.centrexcursionistalcoi.app.platform.ui.getPlatformTextStyles
import org.centrexcursionistalcoi.app.route.Settings
import org.centrexcursionistalcoi.app.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource

object SettingsScreen : Screen<Settings, SettingsViewModel>(::SettingsViewModel) {
    @Composable
    override fun Content(viewModel: SettingsViewModel) {
        val navigator = LocalNavController.current
        val uriHandler = LocalUriHandler.current

        PlatformScaffold(
            onBack = navigator::navigateUp
        ) {
            if (PlatformLanguage.isLanguageFetchSupported) {
                val language = PlatformLanguage.getSelectedLanguage()
                var showingLanguageChangeDialog by remember { mutableStateOf(false) }

                if (showingLanguageChangeDialog) {
                    PlatformDialog(
                        onDismissRequest = { showingLanguageChangeDialog = false }
                    ) {
                        AppText(
                            text = stringResource(Res.string.settings_language),
                            style = getPlatformTextStyles().heading,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                        for (lang in languages) {
                            PlatformSettingsItem(
                                title = PlatformLanguage.localizedNameForTag(lang),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    PlatformLanguage.changeAppLanguage(lang)
                                    showingLanguageChangeDialog = false
                                }
                            )
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

            PlatformSettings()

            PlatformSettingsItem(
                title = stringResource(Res.string.settings_version),
                summary = "${BuildKonfig.VERSION} (${BuildKonfig.CODE})",
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
    }
}
