package org.centrexcursionistalcoi.app.ui.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cea_app.composeapp.generated.resources.*
import org.centrexcursionistalcoi.app.ui.reusable.LazyColumnWidthWrapper
import org.centrexcursionistalcoi.app.ui.reusable.buttons.BackButton
import org.centrexcursionistalcoi.app.ui.reusable.settings.SettingsOptionsRow
import org.jetbrains.compose.resources.stringResource

private val availableLanguages = listOf("en" to "English", "ca" to "Català")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
            item {
                SettingsOptionsRow(
                    title = "",
                    options = availableLanguages,
                    onOptionSelected = {},
                    toString = { it.second },
                )
            }
        }
    }
}
