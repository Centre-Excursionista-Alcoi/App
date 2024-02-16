package ui.pages.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import ui.reusable.navigation.ScaffoldPage

@OptIn(ExperimentalFoundationApi::class)
class SettingsPage : ScaffoldPage() {
    override val icon: ImageVector = Icons.Outlined.Settings

    @Composable
    override fun label(): String = stringResource(Res.string.nav_main_settings)

    @Composable
    override fun PagerScope.PageContent() {
        Text("Settings Page")
    }
}
