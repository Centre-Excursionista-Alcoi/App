package ui.pages.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.stringResource
import ui.reusable.navigation.ScaffoldPage

@OptIn(ExperimentalFoundationApi::class)
class ProfilePage : ScaffoldPage() {
    override val icon: ImageVector = Icons.Outlined.Person

    @Composable
    override fun label(): String = stringResource(Res.string.nav_main_profile)

    @Composable
    override fun PagerScope.PageContent() {
        Text("Profile Page")
    }
}
