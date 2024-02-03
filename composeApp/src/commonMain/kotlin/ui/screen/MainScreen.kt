package ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import cafe.adriel.voyager.core.screen.Screen
import dev.icerock.moko.resources.compose.stringResource
import resources.MR
import ui.reusable.AdaptiveScaffold
import ui.reusable.navigation.ScaffoldPage

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
class MainScreen : Screen {
    private data object RentalPage: ScaffoldPage() {
        override val icon: ImageVector = Icons.Outlined.EditNote

        @Composable
        override fun label(): String = stringResource(MR.strings.nav_main_rental)

        @Composable
        override fun PagerScope.PageContent() {
            Text("Rental Page")
        }
    }

    private data object ProfilePage: ScaffoldPage() {
        override val icon: ImageVector = Icons.Outlined.Person

        @Composable
        override fun label(): String = stringResource(MR.strings.nav_main_profile)

        @Composable
        override fun PagerScope.PageContent() {
            Text("Profile Page")
        }
    }

    private data object SettingsPage: ScaffoldPage() {
        override val icon: ImageVector = Icons.Outlined.Settings

        @Composable
        override fun label(): String = stringResource(MR.strings.nav_main_settings)

        @Composable
        override fun PagerScope.PageContent() {
            Text("Settings Page")
        }
    }

    @Composable
    override fun Content() {
        AdaptiveScaffold(
            pages = listOf(
                RentalPage, ProfilePage, SettingsPage
            )
        )
    }
}
