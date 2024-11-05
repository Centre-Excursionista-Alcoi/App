package org.centrexcursionistalcoi.app.screen

import androidx.compose.runtime.Composable
import org.centrexcursionistalcoi.app.composition.AccountStateNavigator
import org.centrexcursionistalcoi.app.platform.ui.PlatformButton
import org.centrexcursionistalcoi.app.route.Home
import org.centrexcursionistalcoi.app.route.Loading
import org.centrexcursionistalcoi.app.viewmodel.HomeViewModel

object HomeScreen: Screen<Home, HomeViewModel>(::HomeViewModel) {
    @Composable
    override fun Content(viewModel: HomeViewModel) {
        AccountStateNavigator(onLoggedOut = Loading)

        PlatformButton("Logout") {
            viewModel.logout()
        }
    }
}
